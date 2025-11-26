package julianh06.wynnextras.features.profileviewer.data;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class ApiAspect {
    private String name;
    private Icon icon;
    private String rarity;
    private String requiredClass;
    private Map<String, Tier> tiers;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getRequiredClass() {
        return requiredClass;
    }

    public void setRequiredClass(String requiredClass) {
        this.requiredClass = requiredClass;
    }

    public Map<String, Tier> getTiers() {
        return tiers;
    }

    public void setTiers(Map<String, Tier> tiers) {
        this.tiers = tiers;
    }

    public static class Icon {
        private String valueString;
        private IconValue valueObject;
        private String format;

        public String getValueString() {
            return valueString;
        }

        public void setValueString(String valueString) {
            this.valueString = valueString;
        }

        public IconValue getValueObject() {
            return valueObject;
        }

        public void setValueObject(IconValue valueObject) {
            this.valueObject = valueObject;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public boolean isString() {
            return valueString != null;
        }

        public boolean isObject() {
            return valueObject != null;
        }
    }


    public static class IconValue {
        private String id;
        private String name;
        private CustomModelData customModelData;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public CustomModelData getCustomModelData() {
            return customModelData;
        }

        public void setCustomModelData(CustomModelData customModelData) {
            this.customModelData = customModelData;
        }

        public static class CustomModelData {
            private List<Integer> rangeDispatch;

            public List<Integer> getRangeDispatch() { return rangeDispatch; }
            public void setRangeDispatch(List<Integer> rangeDispatch) { this.rangeDispatch = rangeDispatch; }
        }
    }

    public static class Tier {
        private int threshold;
        private List<String> description;

        public int getThreshold() {
            return threshold;
        }

        public void setThreshold(int threshold) {
            this.threshold = threshold;
        }

        public List<String> getDescription() {
            return description;
        }

        public void setDescription(List<String> description) {
            this.description = description;
        }
    }

    public static class IconDeserializer implements JsonDeserializer<Icon> {
        @Override
        public Icon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
            Icon icon = new Icon();
            JsonObject obj = json.getAsJsonObject();

            JsonElement valueElement = obj.get("value");
            if (valueElement != null) {
                if (valueElement.isJsonPrimitive()) {
                    icon.setValueString(valueElement.getAsString());
                } else if (valueElement.isJsonObject()) {
                    icon.setValueObject(ctx.deserialize(valueElement, IconValue.class));
                }
            }

            if (obj.has("format")) {
                icon.setFormat(obj.get("format").getAsString());
            }

            return icon;
        }
    }
}
