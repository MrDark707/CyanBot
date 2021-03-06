package me.xjcyan1de.cyanbot.gui;

import me.xjcyan1de.cyanbot.gui.commands.Command;
import me.xjcyan1de.cyanbot.gui.commands.CommandChat;
import me.xjcyan1de.cyanbot.gui.commands.CommandSpin;
import me.xjcyan1de.cyanbot.gui.commands.CommandWalk;

import javax.swing.*;

public class CommandListHandler {
    public static void createPanel(JPanel commandPanel, int selected) {
        commandPanel.removeAll();
        final Command command = Command.commandList.get(selected);
        command.initPanel(commandPanel);
        commandPanel.updateUI();
    }

    public static void init(DefaultListModel<String> listModel) {
        new CommandChat().register(listModel);
        new CommandWalk().register(listModel);
        new CommandSpin().register(listModel);
    }
}
