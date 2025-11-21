package julianh06.wynnextras.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Optional;

public class OptionalTypeAdapter<T> extends TypeAdapter<Optional<T>> {
    private final TypeAdapter<T> delegate;

    public OptionalTypeAdapter(TypeAdapter<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void write(JsonWriter out, Optional<T> value) throws IOException {
        if (value == null || !value.isPresent()) {
            out.nullValue();
        } else {
            delegate.write(out, value.get());
        }
    }

    @Override
    public Optional<T> read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return Optional.empty();
        }
        return Optional.of(delegate.read(in));
    }
}