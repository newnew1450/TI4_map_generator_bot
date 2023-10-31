package ti4.model;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import java.util.Map;
import java.util.Optional;

@Data
public class StrategyCardModel implements ModelInterface {
    private String name;
    private String alias;
    private Map<Integer, String> cardValues;
    private String description;    
    private Map<Integer, Integer> scNumtoButtonConversion; //first int is CustomSC#, second is BaseSC button equiv. Base SC deck will be <1,1> <2,2> etc.

    @Override
    public boolean isValid() {
        return cardValues.size() > 0 
            && StringUtils.isNotBlank(name)
            && StringUtils.isNotBlank(alias);
    }

    @Override
    public String getAlias() {
        return alias;
    }

    public String getSCName(int scNumber) {
        return cardValues.get(scNumber);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Integer getBaseSCNum(int scNumber) {
            return scNumtoButtonConversion.get(Integer.valueOf(scNumber));
    }
}
