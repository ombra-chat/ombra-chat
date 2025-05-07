module com.twelvemonkeys.imageio.core {
    requires java.desktop;
    requires com.twelvemonkeys.common.lang;
    exports com.twelvemonkeys.imageio;
    opens com.twelvemonkeys.imageio;
    exports com.twelvemonkeys.imageio.color;
    exports com.twelvemonkeys.imageio.spi;
    exports com.twelvemonkeys.imageio.stream;
    exports com.twelvemonkeys.imageio.util;
}