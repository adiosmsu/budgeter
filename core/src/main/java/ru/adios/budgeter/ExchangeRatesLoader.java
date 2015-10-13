package ru.adios.budgeter;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.joda.money.CurrencyUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import ru.adios.budgeter.api.Treasury;
import ru.adios.budgeter.api.UtcDay;

import javax.annotation.concurrent.ThreadSafe;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

/**
 * Date: 6/13/15
 * Time: 11:12 PM
 *
 * @author Mikhail Kulikov
 */
@ThreadSafe
public class ExchangeRatesLoader {

    public static ExchangeRatesLoader.CbrLoader createCbrLoader(Treasury currenciesRepo) {
        return new CbrLoader(currenciesRepo, CurrencyUnit.of(CbrLoader.CODE_RUB), new CbrParser());
    }

    public static ExchangeRatesLoader.BtcLoader createBtcLoader(Treasury currenciesRepo) {
        return new BtcLoader(currenciesRepo, CurrencyUnit.of(BtcLoader.CODE_BTC), new BtcParser());
    }

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRatesLoader.class);

    private static final int HTTP_TIMEOUT_MS = 15000;


    private final Treasury currenciesRepo;
    private final CopyOnWriteArrayList<CurrencyUnit> supportedCurrencies = new CopyOnWriteArrayList<>();
    private final CurrencyUnit mainUnit;

    private final Parser parser;

    private ExchangeRatesLoader(Treasury currenciesRepo, CurrencyUnit mainUnit, Parser parser) {
        this.currenciesRepo = currenciesRepo;
        this.mainUnit = mainUnit;
        this.parser = parser;
    }

    public final CurrencyUnit getMainUnit() {
        return mainUnit;
    }

    public final void updateSupportedCurrencies() {
        currenciesRepo.streamRegisteredCurrencies().forEach(supportedCurrencies::addIfAbsent);
    }

    public final boolean directionFromMainToMapped() {
        return parser.directionFromMainToMapped();
    }

    public final Map<CurrencyUnit, BigDecimal> loadCurrencies(boolean updateSupported, Optional<UtcDay> dayRef, Optional<List<CurrencyUnit>> problematicsRef) {
        if (updateSupported) {
            updateSupportedCurrencies();
        }

        final UtcDay utcDay = dayRef.orElseGet(UtcDay::new);
        final Map<CurrencyUnit, BigDecimal> mergedResults = new TreeMap<>();

        for (String urlStr : parser.getUrlStrings(utcDay, problematicsRef, this)) {
            HttpURLConnection connection = null;
            InputStream is = null;
            Map<CurrencyUnit, BigDecimal> result = null;

            final Optional<Map<CurrencyUnit, BigDecimal>> resultRef = parser.maybeFromCache(urlStr, utcDay, problematicsRef, this);
            if (resultRef.isPresent()) {
                result = resultRef.get();
            } else {
                final long start = System.currentTimeMillis();
                try {
                    final URL url = new URL(urlStr);
                    connection = (HttpURLConnection) url.openConnection();

                    connection.setInstanceFollowRedirects(false);
                    connection.setConnectTimeout(HTTP_TIMEOUT_MS);
                    connection.setReadTimeout(HTTP_TIMEOUT_MS);
                    connection.addRequestProperty("User-Agent", "ru.adios.budgeter-core");
                    connection.addRequestProperty("Accept-Encoding", "gzip");
                    connection.connect();

                    final int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        final String contentEncoding = connection.getContentEncoding();

                        is = new BufferedInputStream(connection.getInputStream(), 1024);
                        if ("gzip".equalsIgnoreCase(contentEncoding))
                            is = new GZIPInputStream(is);

                        result = parser.parseInput(is, urlStr, utcDay, problematicsRef, this);

                        logger.info("fetched exchange rates from {} ({}), took {} ms", url, contentEncoding, System.currentTimeMillis() - start);
                    } else {
                        logger.warn("http status {} when fetching exchange rates from {}", responseCode, url);
                    }
                }
                catch (Throwable th) {
                    logger.warn("problem fetching exchange rates from " + urlStr, th);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException ignore) {}
                    }

                    if (connection != null)
                        connection.disconnect();
                }
            }

            if (result != null) {
                if (parser.resultInOneQuery(urlStr)) {
                    return result;
                } else {
                    mergedResults.putAll(result);
                }
            }
        }

        return mergedResults;
    }

    public final Map<CurrencyUnit, BigDecimal> loadCurrencies(Optional<UtcDay> dayRef, Optional<List<CurrencyUnit>> problematicsRef) {
        return loadCurrencies(true, dayRef, problematicsRef);
    }

    boolean isFetchingAllSupportedProblematic(UtcDay day) {
        return false;
    }


    private interface Parser {

        List<String> getUrlStrings(UtcDay day, Optional<List<CurrencyUnit>> problematicsRef, ExchangeRatesLoader loader);

        boolean resultInOneQuery(String urlStr);

        Optional<Map<CurrencyUnit, BigDecimal>> maybeFromCache(String urlStr, UtcDay day, Optional<List<CurrencyUnit>> problematicsRef, ExchangeRatesLoader loader);

        Map<CurrencyUnit, BigDecimal> parseInput(InputStream reader, String urlStr, UtcDay day, Optional<List<CurrencyUnit>> problematicsRef, ExchangeRatesLoader loader) throws IOException;

        boolean directionFromMainToMapped();

    }

    @ThreadSafe
    public static final class CbrLoader extends ExchangeRatesLoader {

        private static final String CODE_RUB = "RUB";
        private static final String CBR_ADDRESS = "http://www.cbr.ru/scripts/XML_daily.asp?date_req=";
        private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        private CbrLoader(Treasury currenciesRepo, CurrencyUnit mainUnit, CbrParser cbrParser) {
            super(currenciesRepo, mainUnit, cbrParser);
        }

    }

    @ThreadSafe
    private final static class CbrParser implements Parser {

        private CbrParser() {}

        @Override
        public List<String> getUrlStrings(UtcDay day, Optional<List<CurrencyUnit>> problematicsRef, ExchangeRatesLoader loader) {
            return ImmutableList.of(CbrLoader.CBR_ADDRESS + CbrLoader.DAY_FORMATTER.format(day.inner));
        }

        @Override
        public boolean resultInOneQuery(String urlStr) {
            return true;
        }

        @Override
        public boolean directionFromMainToMapped() {
            return false;
        }

        @Override
        public Optional<Map<CurrencyUnit, BigDecimal>> maybeFromCache(String urlStr, UtcDay day, Optional<List<CurrencyUnit>> problematicsRef, ExchangeRatesLoader loader) {
            return Optional.empty();
        }

        @Override
        public Map<CurrencyUnit, BigDecimal> parseInput(
                InputStream stream,
                String urlStr,
                UtcDay day,
                final Optional<List<CurrencyUnit>> problematicsRef,
                final ExchangeRatesLoader loader
        ) throws IOException {
            final Map<CurrencyUnit, BigDecimal> rates = new TreeMap<>();

            final Optional<StringBuilder> debugDump = logger.isDebugEnabled()
                    ? Optional.of(new StringBuilder(1000))
                    : Optional.empty();
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            try {
                final SAXParser saxParser = factory.newSAXParser();

                final InputStreamReader r = new InputStreamReader(stream, "windows-1251");
                final InputSource inputSource = new InputSource(r);
                saxParser.parse(inputSource, new DefaultHandler() {
                    private boolean insideCode = false;
                    private boolean insideValue = false;

                    private CurrencyUnit currentUnit;
                    private BigDecimal currentRate;

                    @Override
                    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                        if (qName.equalsIgnoreCase("CharCode")) {
                            insideCode = true;
                        } else if (qName.equalsIgnoreCase("Value")) {
                            insideValue = true;
                        }
                    }

                    @Override
                    public void characters(final char[] ch, final int start, final int length) throws SAXException {
                        debugDump.ifPresent(builder -> builder.append(new String(ch, start, length)));

                        if (insideCode) {
                            final String code = new String(ch, start, length).toUpperCase();
                            if (loader.supportedCurrency(code, problematicsRef)) {
                                currentUnit = CurrencyUnit.getInstance(code);
                            }
                            insideCode = false;
                        } else if (insideValue) {
                            final String value = new String(ch, start, length).replace(',', '.');
                            currentRate = new BigDecimal(value);
                            insideValue = false;
                        }

                        if (currentUnit != null && currentRate != null) {
                            rates.put(currentUnit, currentRate);
                            currentUnit = null;
                            currentRate = null;
                        }
                    }

                    @Override
                    public void endElement(String uri, String localName, String qName) throws SAXException {
                        if (qName.equalsIgnoreCase("Valute")) {
                            currentRate = null;
                        }
                    }
                });
            } catch (ParserConfigurationException | SAXException e) {
                throw new IOException(e);
            }

            debugDump.ifPresent(builder -> logger.debug(builder.toString()));

            return rates;
        }

    }

    @ThreadSafe
    public static final class BtcLoader extends ExchangeRatesLoader {

        private static final String BITCOIN_AVERAGE_URL_STR = "https://api.bitcoinaverage.com/custom/abw";
        private static final String BLOCKCHAIN_INFO_URL_STR = "https://blockchain.info/ticker";
        private static final String BITCOIN_AVERAGE_HISTORY_START = "https://api.bitcoinaverage.com/history/";
        private static final String BITCOIN_AVERAGE_HISTORY_URL_TEMPLATE = BITCOIN_AVERAGE_HISTORY_START + "<>/per_day_all_time_history.csv";
        private static final String CODE_BTC = "BTC";
        private static final String CODE_MBTC = "mBTC";
        private static final String CODE_UBTC = "µBTC";
        private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withChronology(IsoChronology.INSTANCE);

        private BtcLoader(Treasury currenciesRepo, CurrencyUnit mainUnit, BtcParser btcParser) {
            super(currenciesRepo, mainUnit, btcParser);
        }

        @Override
        boolean isFetchingAllSupportedProblematic(UtcDay day) {
            return !day.equals(new UtcDay());
        }

    }

    @ThreadSafe
    private final static class BtcParser implements Parser {

        private final ConcurrentHashMap<CurrencyUnit, ConcurrentHashMap<UtcDay, BigDecimal>> historyCache = new ConcurrentHashMap<>(20, 1.0f, 2);
        private final AtomicReference<UtcDay> lastCacheUpdate = new AtomicReference<>(new UtcDay());

        private BtcParser() {}

        @Override
        public List<String> getUrlStrings(UtcDay day, Optional<List<CurrencyUnit>> problematicsRef, ExchangeRatesLoader loader) {
            if (day.equals(new UtcDay())) {
                return ImmutableList.of(BtcLoader.BLOCKCHAIN_INFO_URL_STR, BtcLoader.BITCOIN_AVERAGE_URL_STR);
            } else {
                final List<CurrencyUnit> relevant = loader.supportedFromOptional(problematicsRef);
                return relevant
                        .stream()
                        .map(unit -> BtcLoader.BITCOIN_AVERAGE_HISTORY_URL_TEMPLATE.replace("<>", unit.getCode().toUpperCase()))
                        .collect(Collectors.toList());
            }
        }

        @Override
        public boolean directionFromMainToMapped() {
            return true;
        }

        @Override
        public boolean resultInOneQuery(String urlStr) {
            return !urlStr.startsWith(BtcLoader.BITCOIN_AVERAGE_HISTORY_START);
        }

        @Override
        public Optional<Map<CurrencyUnit, BigDecimal>> maybeFromCache(String urlStr, UtcDay day, Optional<List<CurrencyUnit>> problematicsRef, ExchangeRatesLoader loader) {
            if (urlStr.startsWith(BtcLoader.BITCOIN_AVERAGE_HISTORY_START)) {
                final UtcDay lcuSnapshot = lastCacheUpdate.get();
                final UtcDay today = new UtcDay();

                if (lcuSnapshot.compareTo(today) >= 0) {
                    final Map<CurrencyUnit, BigDecimal> result = new TreeMap<>();

                    for (final CurrencyUnit unit : loader.supportedFromOptional(problematicsRef)) {
                        final ConcurrentHashMap<UtcDay, BigDecimal> unitCache = historyCache.get(unit);

                        if (unitCache == null)
                            return Optional.empty();

                        final BigDecimal rate = unitCache.get(day);

                        if (rate == null) {
                            return Optional.empty();
                        }

                        result.put(unit, rate);
                    }

                    return Optional.of(result);
                } else {
                    lastCacheUpdate.compareAndSet(lcuSnapshot, today);
                }
            }
            return Optional.empty();
        }

        @Override
        public Map<CurrencyUnit, BigDecimal> parseInput(
                InputStream stream,
                String urlStr,
                UtcDay day,
                Optional<List<CurrencyUnit>> problematicsRef,
                ExchangeRatesLoader loader
        ) throws IOException {
            final Map<CurrencyUnit, BigDecimal> rates = new TreeMap<>();

            if (urlStr.startsWith(BtcLoader.BITCOIN_AVERAGE_HISTORY_START)) {
                final String code = urlStr.replace(BtcLoader.BITCOIN_AVERAGE_HISTORY_START, "").substring(0, 3);
                final CurrencyUnit unit = CurrencyUnit.of(code);

                final ConcurrentHashMap<UtcDay, BigDecimal> unitCache = historyCache.computeIfAbsent(unit, key -> new ConcurrentHashMap<>(1000, 0.75f, 4));
                parseCsv(stream).forEach(
                        dayAndRate -> unitCache.putIfAbsent(dayAndRate.day, dayAndRate.rate)
                );
                rates.put(unit, unitCache.get(day));
            } else {
                final JsonFactory jsonFactory = new JsonFactory();
                final JsonParser jsonParser = jsonFactory.createJsonParser(stream);

                JsonToken token;
                while ((token = jsonParser.nextToken()) != null) {
                    if (token == JsonToken.FIELD_NAME) {
                        final String code = jsonParser.getText();
                        if (code != null && !"timestamp".equals(code) && loader.supportedCurrency(code, problematicsRef) && !BtcLoader.CODE_BTC.equals(code)
                                && !BtcLoader.CODE_MBTC.equals(code) && !BtcLoader.CODE_UBTC.equals(code))
                        {
                            while ((token = jsonParser.nextToken()) != JsonToken.END_OBJECT) {
                                if (token == JsonToken.FIELD_NAME) {
                                    final String fieldName = jsonParser.getText();

                                    if ("15m".equals(fieldName) || "24h_avg".equals(fieldName) || "last".equals(fieldName)) {
                                        if (jsonParser.nextToken() == JsonToken.VALUE_NUMBER_FLOAT) {
                                            // number
                                            final BigDecimal rate = jsonParser.getDecimalValue();
                                            if (rate != null) {
                                                rates.put(CurrencyUnit.of(code), rate);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return rates;
        }

        private Stream<DayAndRate> parseCsv(InputStream is) throws IOException {
            final CSVParser parser = new CSVParser(new InputStreamReader(is, Charsets.UTF_8), CSVFormat.RFC4180);
            final ParsePosition pos = new ParsePosition(0);
            return StreamSupport
                    .stream(parser.spliterator(), false)
                    .map(record -> {
                        final String str = record.get(0);
                        BtcLoader.CSV_DATE_FORMATTER.parseUnresolved(str, pos);
                        try {
                            if (pos.getErrorIndex() < 0) {
                                try {
                                    return Optional.of(new DayAndRate(
                                                    new UtcDay(OffsetDateTime.of(LocalDateTime.parse(str, BtcLoader.CSV_DATE_FORMATTER), ZoneOffset.UTC)),
                                                    new BigDecimal(record.get(3)))
                                    );
                                } catch (DateTimeException ignore) {}
                            }
                            return Optional.<DayAndRate>empty();
                        } finally {
                            pos.setIndex(0);
                            pos.setErrorIndex(-1);
                        }
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get);
        }

    }

    private static final class DayAndRate {
        private final UtcDay day;
        private final BigDecimal rate;

        private DayAndRate(UtcDay day, BigDecimal rate) {
            this.day = day;
            this.rate = rate;
        }
    }

    private ImmutableList<CurrencyUnit> getSupportedCurrenciesSnapshot() {
        return ImmutableList.copyOf(supportedCurrencies);
    }

    private List<CurrencyUnit> supportedFromOptional(Optional<List<CurrencyUnit>> problematicsRef) {
        return problematicsRef.orElseGet(this::getSupportedCurrenciesSnapshot);
    }

    private boolean supportedCurrency(String code, Optional<List<CurrencyUnit>> problematicsRef) {
        if (problematicsRef.isPresent()) {
            return _supportedCurrency(problematicsRef.get(), code);
        }
        return _supportedCurrency(supportedCurrencies, code);
    }

    private static boolean _supportedCurrency(List<CurrencyUnit> curs, String code) {
        for (final CurrencyUnit unit : curs) {
            if (unit.getCode().equals(code))
                return true;
        }
        return false;
    }

}
