package julianh06.wynnextras.config;

import com.google.gson.annotations.SerializedName;

public enum ScaleBackgroundShape {
    @SerializedName(value = "CIRCLE", alternate = {"CIRCLE_OUTLINE_LARGE", "CIRCLE_OUTLINE_SMALL"})
    CIRCLE,
    @SerializedName(value = "BOX", alternate = {"BOX_GRADIENT_1", "BOX_GRADIENT_2", "WYNN", "TAG"})
    BOX
}
