package li.gerard.crystallomachia.golem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record GolemRole(Map<GolemCapability, Integer> requirements) {
    public GolemRole {
        requirements = Collections.unmodifiableMap(new EnumMap<>(requirements));
    }

    public static GolemRole of(GolemCapability capability, int level) {
        return new GolemRole(Map.of(capability, level));
    }

    public static GolemRole of(GolemCapability first, int firstLevel, GolemCapability second, int secondLevel) {
        return new GolemRole(Map.of(first, firstLevel, second, secondLevel));
    }

    public List<GolemCapability> capabilities() {
        return new ArrayList<>(this.requirements.keySet());
    }

    public int required(GolemCapability capability) {
        return this.requirements.getOrDefault(capability, 0);
    }

    public boolean isMetBy(GolemData golem) {
        for (Map.Entry<GolemCapability, Integer> requirement : this.requirements.entrySet()) {
            if (golem.level(requirement.getKey()) < requirement.getValue()) {
                return false;
            }
        }
        return true;
    }

    public int surplus(GolemData golem) {
        int surplus = 0;
        for (Map.Entry<GolemCapability, Integer> requirement : this.requirements.entrySet()) {
            surplus += Math.max(0, golem.level(requirement.getKey()) - requirement.getValue());
        }
        return surplus;
    }
}
