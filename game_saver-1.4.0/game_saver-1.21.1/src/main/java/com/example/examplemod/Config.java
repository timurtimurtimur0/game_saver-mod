package com.example.examplemod;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MAX_LOG_SIZE;
    public static final ModConfigSpec.IntValue MAX_CHUNK_SIZE;
    static {
        BUILDER.push("Log Settings");

        MAX_LOG_SIZE = BUILDER.comment("Максимальное количество изменений блоков сохраняемых в лог.",
                        "Оптимальное значение: от 5000 до 30000. Большие значения могут вызвать лаги при откате мира.",
                        "The maximum number of block changes saved to the log.",
                        "Optimal value: 5,000 to 30,000. Higher values may cause lag during world rollback.")
                .defineInRange("maxLogSize", 16000, 1000, 100000);
        MAX_CHUNK_SIZE = BUILDER.comment("Максимальное количество чанков сохраняемых в лог.",
                "Оптимальное значение: от 150 до 200. Большие значения могут вызвать лаги при откате мира.",
                "The maximum number of chunks saved in the log.",
                "Optimal value: 150 to 200. Higher values may cause lag during world rollback.")
                .defineInRange("maxChunkSize", 1000, 50, 100000);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
