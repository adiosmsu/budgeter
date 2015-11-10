package ru.adios.budgeter;

import java8.util.Optional;
import java8.util.function.Supplier;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adios.budgeter.api.FundsMutationSubjectRepository;
import ru.adios.budgeter.api.SubjectPriceRepository;
import ru.adios.budgeter.api.TransactionalSupport;
import ru.adios.budgeter.api.UtcDay;
import ru.adios.budgeter.api.data.FundsMutationAgent;
import ru.adios.budgeter.api.data.FundsMutationSubject;
import ru.adios.budgeter.api.data.SubjectPrice;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * Date: 11/10/15
 * Time: 7:39 AM
 *
 * @author Mikhail Kulikov
 */
public class PriceAdditionElementCore implements Submitter<SubjectPrice> {

    public static final String FIELD_DAY = "day";
    public static final String FIELD_PRICE = "price";
    public static final String FIELD_PRICE_UNIT = "priceUnit";
    public static final String FIELD_PRICE_AMOUNT = "priceAmount";
    public static final String FIELD_SUBJECT = "subject";
    public static final String FIELD_AGENT = "agent";

    private static final Logger logger = LoggerFactory.getLogger(PriceAdditionElementCore.class);


    private final SubjectPriceRepository priceRepository;
    private final FundsMutationSubjectRepository subjectRepository;
    private final SubmitHelper<SubjectPrice> helper = new SubmitHelper<SubjectPrice>(logger, "Error while adding new subject price");

    private final SubjectPrice.Builder priceBuilder = SubjectPrice.builder();
    private final MoneyPositiveWrapper priceWrapper = new MoneyPositiveWrapper("subject price");
    private String subjectName;

    private PriceSettable priceSettable;
    private boolean lockOn = false;
    private Result<SubjectPrice> storedResult;

    public PriceAdditionElementCore(SubjectPriceRepository priceRepository, FundsMutationSubjectRepository subjectRepository) {
        this.priceRepository = priceRepository;
        this.subjectRepository = subjectRepository;
    }

    public PriceSettable getPriceSettable() {
        if (priceSettable == null) {
            priceSettable = new PriceSettable();
        }
        return priceSettable;
    }

    public void setDay(UtcDay timestamp) {
        if (lockOn) return;
        priceBuilder.setDay(timestamp);
    }

    @Nullable
    public UtcDay getDay() {
        return priceBuilder.getDay();
    }

    @Nullable
    public FundsMutationAgent getAgent() {
        return priceBuilder.getAgent();
    }

    public void setAgent(FundsMutationAgent agent) {
        if (lockOn) return;
        priceBuilder.setAgent(agent);
    }

    @Nullable
    public FundsMutationSubject getSubject() {
        return priceBuilder.getSubject();
    }

    public void setSubject(FundsMutationSubject subject) {
        if (lockOn) return;
        priceBuilder.setSubject(subject);
    }

    @PotentiallyBlocking
    public boolean setSubjectName(String subjectName) {
        if (lockOn) return false;

        this.subjectName = subjectName;

        if (subjectName == null) {
            priceBuilder.setSubject(null);
            return true;
        }

        final Optional<FundsMutationSubject> subjectRef = subjectRepository.findByName(subjectName);
        if (subjectRef.isPresent()) {
            priceBuilder.setSubject(subjectRef.get());
            return true;
        }

        return false;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setPrice(int coins, int cents) {
        if (lockOn) return;
        priceWrapper.setAmount(coins, cents);
    }

    public void setPrice(Money amount) {
        if (lockOn) return;
        priceWrapper.setAmount(amount);
    }

    public Money getPrice() {
        return priceWrapper.getAmount();
    }

    public void setPriceDecimal(BigDecimal amountDecimal) {
        if (lockOn) return;
        priceWrapper.setAmountDecimal(amountDecimal);
    }

    public BigDecimal getPriceDecimal() {
        return priceWrapper.getAmountDecimal();
    }

    public void setPriceUnit(String code) {
        if (lockOn) return;
        priceWrapper.setAmountUnit(code);
    }

    public void setPriceUnit(CurrencyUnit unit) {
        if (lockOn) return;
        priceWrapper.setAmountUnit(unit);
    }

    public CurrencyUnit getPriceUnit() {
        return priceWrapper.getAmountUnit();
    }

    @PotentiallyBlocking
    @Override
    public Result<SubjectPrice> submit() {
        final Submitter.ResultBuilder<SubjectPrice> resultBuilder = new ResultBuilder<SubjectPrice>();
        resultBuilder.addFieldErrorIfNull(priceBuilder.getDay(), FIELD_DAY)
                .addFieldErrorIfNull(priceBuilder.getSubject(), FIELD_SUBJECT)
                .addFieldErrorIfNull(priceBuilder.getAgent(), FIELD_AGENT)
                .addFieldErrorIfNotSet(priceWrapper, FIELD_PRICE, FIELD_PRICE_UNIT, FIELD_PRICE_AMOUNT);

        if (resultBuilder.toBuildError()) {
            return resultBuilder.build();
        }

        return helper.doSubmit(new Supplier<Result<SubjectPrice>>() {
            @Override
            public Result<SubjectPrice> get() {
                return doSubmit();
            }
        }, resultBuilder);
    }

    @Nonnull
    private Result<SubjectPrice> doSubmit() {
        priceBuilder.setPrice(priceWrapper.getAmount());
        final SubjectPrice p = priceBuilder.build();
        priceRepository.register(p);
        return Result.success(p);
    }

    @Override
    public void setTransactional(TransactionalSupport transactional) {
        helper.setTransactionalSupport(transactional);
    }

    @Override
    public TransactionalSupport getTransactional() {
        return helper.getTransactionalSupport();
    }

    @Override
    public void lock() {
        lockOn = true;
    }

    @Override
    public void unlock() {
        lockOn = false;
    }

    @Override
    public Result<SubjectPrice> getStoredResult() {
        return storedResult;
    }

    @Override
    public void submitAndStoreResult() {
        storedResult = submit();
    }

    public final class PriceSettable implements MoneySettable {

        private PriceSettable() {}

        @Override
        public void setAmount(int coins, int cents) {
            setPrice(coins, cents);
        }

        @Override
        public void setAmountDecimal(BigDecimal amountDecimal) {
            setPriceDecimal(amountDecimal);
        }

        @Override
        public BigDecimal getAmountDecimal() {
            return getPriceDecimal();
        }

        @Override
        public void setAmountUnit(String code) {
            setPriceUnit(code);
        }

        @Override
        public void setAmountUnit(CurrencyUnit unit) {
            setPriceUnit(unit);
        }

        @Override
        public CurrencyUnit getAmountUnit() {
            return getPriceUnit();
        }

        @Override
        public void setAmount(Money amount) {
            setPrice(amount);
        }

        @Override
        public Money getAmount() {
            return getPrice();
        }

    }

}
