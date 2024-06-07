package glide.api.models.commands;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;
import java.util.List;

@SuperBuilder
public final class SortStandaloneOptions extends SortOptions {
    private final ByPattern byPattern;

    @RequiredArgsConstructor
    public static final class ByPattern {
        private final String pattern;
    }

    public String[] toArgs() {
        List<String> optionArgs = Arrays.asList(super.toArgs());

        if (byPattern != null) {
            optionArgs.addAll(List.of(BY_COMMAND_STRING, byPattern.pattern));
        }

        return optionArgs.toArray(new String[0]);
    }
}


