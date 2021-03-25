package lrg.dude.duplication;

import org.dxworks.argumenthor.config.fields.FieldConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IntField extends FieldConfig<Integer> {
	public IntField(@NotNull String name, @Nullable Integer defaultValue) {
		super(name, defaultValue);
	}

	@Nullable
	@Override
	public Integer parse(@Nullable String s) {
		return s != null ? Integer.parseInt(s) : null;
	}
}
