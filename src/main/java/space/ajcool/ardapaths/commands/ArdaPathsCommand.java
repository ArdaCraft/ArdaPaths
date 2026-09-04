package space.ajcool.ardapaths.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.backup.BackupJobRunner;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * Registers and handles ArdaPaths server commands.
 */
public class ArdaPathsCommand {

    /**
     * Async backup job runner used by command executions.
     */
    private static final BackupJobRunner BACKUP_RUNNER = new BackupJobRunner();

    /**
     * Registers the root {@code /ardapaths} command tree.
     *
     * @param dispatcher command dispatcher for the active server
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("ardapaths")
                .requires(source -> Permissions.check(source, ArdaPaths.MOD_EDIT_PERMISSION, 2))
                .then(literal("backup")
                        .executes(ArdaPathsCommand::backup))
                .then(literal("restore")
                        .executes(context -> restore(context, null, false))
                        .then(literal("hard")
                                .executes(context -> restore(context, null, true)))
                        .then(argument("file", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    BACKUP_RUNNER.listBackupZipNames().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> restore(context, StringArgumentType.getString(context, "file"), false))
                                .then(literal("hard")
                                        .executes(context -> restore(context, StringArgumentType.getString(context, "file"), true))))));
    }

    /**
     * Runs a backup command.
     *
     * @param context command context
     * @return command result
     */
    private static int backup(CommandContext<CommandSourceStack> context) {
        BackupJobRunner.JobStartResult result = BACKUP_RUNNER.tryStartBackup(context.getSource());

        if (!result.started()) {
            context.getSource().sendSuccess(() -> Component.literal("An ArdaPaths job is already in progress: " + result.snapshot().format()), false);
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("ArdaPaths backup started; progress in server log."), false);
        return 1;
    }

    /**
     * Runs a restore command.
     *
     * @param context command context
     * @param file    optional historical zip file name
     * @param hard    whether stale markers should be deleted
     * @return command result
     */
    private static int restore(CommandContext<CommandSourceStack> context, String file, boolean hard) {
        BackupJobRunner.JobStartResult result = BACKUP_RUNNER.tryStartRestore(context.getSource(), file, hard);

        if (!result.started()) {
            context.getSource().sendSuccess(() -> Component.literal("An ArdaPaths job is already in progress: " + result.snapshot().format()), false);
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("ArdaPaths restore started" + (hard ? " in hard mode" : "") + "; progress in server log."), false);
        return 1;
    }
}
