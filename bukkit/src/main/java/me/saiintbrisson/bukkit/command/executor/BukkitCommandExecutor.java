package me.saiintbrisson.bukkit.command.executor;

import lombok.Getter;
import lombok.Setter;
import me.saiintbrisson.bukkit.command.BukkitFrame;
import me.saiintbrisson.bukkit.command.command.BukkitCommand;
import me.saiintbrisson.minecraft.command.argument.ArgumentParser;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.exception.CommandException;
import me.saiintbrisson.minecraft.command.executor.CommandExecutor;
import me.saiintbrisson.minecraft.command.message.MessageHolder;
import me.saiintbrisson.minecraft.command.message.MessageType;
import org.bukkit.command.CommandSender;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author SaiintBrisson
 */
public class BukkitCommandExecutor implements CommandExecutor<CommandSender> {

    private final Method method;
    private final Object holder;

    @Getter
    private final ArgumentParser<CommandSender> parser;

    private final MessageHolder messageHolder;

    @Setter
    private BukkitCommand command;

    public BukkitCommandExecutor(BukkitFrame frame, Method method, Object holder) {
        final Class<?> returnType = method.getReturnType();

        if (!returnType.equals(Void.TYPE)
          && !returnType.equals(Boolean.TYPE)) {
            throw new CommandException("Illegal return type, '" + method.getName());
        }

        this.method = method;
        this.holder = holder;

        this.parser = new ArgumentParser<>(frame.getAdapterMap(), method);
        this.messageHolder = frame.getMessageHolder();
    }

    @Override
    public boolean execute(Context<CommandSender> context) {
        final Object result = invokeCommand(context);

        if (result != null && result.getClass().equals(Boolean.TYPE)) {
            return ((boolean) result);
        }

        return false;
    }

    public Object invokeCommand(Context<CommandSender> context) {
        try {
            if (parser.getArgumentList().size() == 0) {
                return method.invoke(holder);
            }

            final Object[] parameters;

            try {
                parameters = parser.parseArguments(context);
            } catch (Exception e) {
                throw new InvocationTargetException(new CommandException(MessageType.INCORRECT_USAGE, null));
            }

            return method.invoke(holder, parameters);
        } catch (InvocationTargetException e) {
            final Throwable throwable = e.getTargetException();

            if (throwable == null || !(throwable instanceof CommandException)) {
                e.printStackTrace();
                context.sendMessage("§cAn internal error occurred, please contact the staff team.");

                return false;
            }

            final CommandException exception = (CommandException) throwable;
            final MessageType messageType = exception.getMessageType();

            String message = throwable.getMessage();

            if (messageType != null) {
                if (message == null) {
                    message = messageType.getDefault(command);
                }

                context.sendMessage(messageHolder.getReplacing(messageType, message));
            } else {
                e.printStackTrace();

                if (e.getMessage() != null) {
                    context.sendMessage(messageHolder.getReplacing(MessageType.ERROR, e.getMessage()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            context.sendMessage("§cAn internal error occurred, please contact the staff team.");
        }

        return false;
    }

}
