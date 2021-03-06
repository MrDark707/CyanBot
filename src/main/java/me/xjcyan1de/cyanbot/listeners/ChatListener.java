package me.xjcyan1de.cyanbot.listeners;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerUseItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import me.xjcyan1de.cyanbot.Bot;
import me.xjcyan1de.cyanbot.events.CommandEvent;
import me.xjcyan1de.cyanbot.utils.Schedule;

import java.util.LinkedList;
import java.util.List;


public class ChatListener extends SessionAdapter {
    private Bot bot;
    private List<String> accessPatterns = new LinkedList<>();

    public ChatListener(Bot bot) {
        this.bot = bot;
    }

    @Override
    public void packetReceived(PacketReceivedEvent event) {
        if (!(event.getPacket() instanceof ServerChatPacket)) return;
        ServerChatPacket packet = event.getPacket();
        final String message = packet.getMessage().getFullText();
//        System.out.println(bot.getUsername() + " <- " + message);
        onMessage(message);
    }

    public void onMessage(String message) {
        final String[] split = message.split(" ");
        int readFrom = -1;
        for (int i = 0; i < split.length; i++) {
            if (split[i].startsWith(bot.getUsername() + ",")) {
                readFrom = i + 1 < split.length ? i + 1 : -1;
                break;
            }
        }
        if (readFrom != -1) {
            command(message, split, readFrom);
        }
    }

    public void command(String senderPattern, String[] split, int readFrom) {
        String[] args = new String[split.length - readFrom];
        System.arraycopy(split, readFrom, args, 0, args.length);

        if (!hasAccess(senderPattern)) {
            if ((args.length == 2 && args[0].equals("ключ") && args[1].equals(bot.getAccessKey())) ||
                    args.length == 1 && args[0].equals(bot.getAccessKey())) {
                int totalLenght = 0;
                for (String arg : args) totalLenght += arg.length();
                final String pattern = senderPattern.substring(0, senderPattern.length() - totalLenght - 1);
                accessPatterns.add(pattern);
                System.out.println("Добавлен патерн: " + pattern);
                bot.sendMessage("Успешная авторизация!");
                bot.generateAccessKey();
            }
        } else {

            final String[] argsWithoutCommand = new String[args.length - 1];
            System.arraycopy(args, 1, argsWithoutCommand, 0, args.length - 1);
            bot.getEventSystem().callEvent(new CommandEvent(bot, args[0], argsWithoutCommand)); // кидаем ивент команды

            switch (args[0].toLowerCase()) {
                case "say":
                case "напиши":
                case "скажи": {
                    if (args.length > 1) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i < args.length; i++) {
                            sb.append(args[i]).append(" ");
                        }
                        sb.setLength(sb.length() - 1);
                        bot.sendMessage(sb.toString());
                    }
                    break;
                }
                case "flood":
                case "флуди": {
                    if (args.length > 1) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i < args.length; i++) {
                            sb.append(args[i]).append(" ");
                        }
                        sb.setLength(sb.length() - 1);
                        Schedule.timer(() -> {
                            bot.sendMessage(sb.toString());
                        }, 1000, 1000);
                    }
                    break;
                }
                case "walk":
                case "иди": {
                    if (args.length != 4) return;
                    double x = Double.parseDouble(args[1]);
                    double y = Double.parseDouble(args[2]);
                    double z = Double.parseDouble(args[3]);

                    bot.getLoc().add(x, y, z);

                    break;
                }
                case "spin":
                case "кружись": {
                    final int[] yaw = {0};
                    Schedule.timer(() -> {
                        bot.getLoc().setYaw(yaw[0]);
                        yaw[0] += 30;
                    }, 50, 50);
                    break;
                }
                case "drop":
                case "выкинь": {
                    bot.sendPacket(new ClientPlayerActionPacket(
                            PlayerAction.DROP_ITEM,
                            new Position(bot.getLoc().getBlockX(), bot.getLoc().getBlockY(), bot.getLoc().getBlockZ()),
                            BlockFace.UP));
                    break;
                }
                case "swap":
                case "переложи": {
                    bot.sendPacket(new ClientPlayerActionPacket(
                            PlayerAction.SWAP_HANDS,
                            new Position(bot.getLoc().getBlockX(), bot.getLoc().getBlockY(), bot.getLoc().getBlockZ()),
                            BlockFace.UP));
                    break;
                }
                case "swapping":
                case "перехватывай": {
                    if (args.length != 2) return;
                    int delay = Integer.parseInt(args[1]);
                    Schedule.timer(() -> {
                        bot.sendPacket(new ClientPlayerActionPacket(
                                PlayerAction.SWAP_HANDS,
                                new Position(bot.getLoc().getBlockX(), bot.getLoc().getBlockY(), bot.getLoc().getBlockZ()),
                                BlockFace.UP));
                    }, delay, delay);
                    break;
                }
                case "rightclick":
                case "пкм": {
                    bot.sendPacket(new ClientPlayerUseItemPacket(Hand.MAIN_HAND));
                    break;
                }
                case "deus": {
                    if (args.length == 2 && args[1].equalsIgnoreCase("vult")) {
                        bot.sendMessage("Ave Maria!");
                    }
                    break;
                }
                case "слава": {
                    if (args.length == 2 && args[1].equalsIgnoreCase("украине")) {
                        bot.sendMessage("ГЕРОЯМ СЛАВА!");
                    }
                    break;
                }
                default: {

                }
            }
        }
    }

    public boolean hasAccess(String pattern) {
        return accessPatterns.stream().anyMatch(pattern::startsWith);
    }
}
