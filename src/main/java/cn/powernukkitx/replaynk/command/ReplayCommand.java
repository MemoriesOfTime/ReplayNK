package cn.powernukkitx.replaynk.command;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.lang.LangCode;
import cn.nukkit.lang.PluginI18nManager;
import cn.powernukkitx.replaynk.ReplayNK;
import cn.powernukkitx.replaynk.trail.Trail;
import cn.powernukkitx.replaynk.trail.TrailAgent;

import java.util.ArrayList;

/**
 * @author daoge_cmd
 * @date 2023/6/16
 * ReplayNK Project
 */
public class ReplayCommand extends PluginCommand<ReplayNK> {
    public ReplayCommand(ReplayNK plugin) {
        super("replaynk", /*"replaynk.command.replay.description", */plugin);
        setAliases(new String[]{"replay", "rp", "rpnk"});
        setPermission("replaynk.command.replay");
        commandParameters.clear();
        commandParameters.put("operate", new CommandParameter[]{
                CommandParameter.newEnum("operate", new String[]{"operate"}),
                CommandParameter.newType("name", false, CommandParamType.STRING)
        });
        commandParameters.put("create", new CommandParameter[]{
                CommandParameter.newEnum("create", new String[]{"create"}),
                CommandParameter.newType("name", false, CommandParamType.STRING)
        });
        commandParameters.put("remove", new CommandParameter[]{
                CommandParameter.newEnum("remove", new String[]{"remove"}),
                CommandParameter.newType("name", false, CommandParamType.STRING)
        });
        commandParameters.put("play", new CommandParameter[]{
                CommandParameter.newEnum("play", new String[]{"play"}),
                CommandParameter.newType("playerName", false, CommandParamType.TARGET),
                CommandParameter.newType("name", false, CommandParamType.STRING)
        });
        commandParameters.put("list", new CommandParameter[]{
                CommandParameter.newEnum("list", new String[]{"list"}),
        });
        //enableParamTree();
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.isPlayer()) {
            this.sendMessage(sender, "replaynk.command.replay.onlyplayer");
            return true;
        }
        if (args.length < 1) {
            return false;
        }
        Player player = sender.asPlayer();
        switch (args[0]) {
            case "operate" -> {
                if (Trail.isOperatingTrail(player)) {
                    this.sendMessage(sender, "replaynk.trail.alreadyoperatingtrail");
                    return true;
                }
                if (args.length < 2) {
                    return false;
                }
                String trailName = args[1];
                var trail = Trail.getTrail(trailName);
                if (trail == null) {
                    this.sendMessage(sender, "replaynk.trail.notfound", trailName);
                    return true;
                }
                trail.startOperating(player);
                this.sendMessage(sender, "replaynk.trail.startoperating", trailName);
                return true;
            }
            case "create" -> {
                if (args.length < 2) {
                    return false;
                }
                String trailName = args[1];
                var trail = Trail.create(trailName);
                if (trail != null) {
                    this.sendMessage(sender, "replaynk.trail.created", trailName);
                    if (!Trail.isOperatingTrail(player)) {
                        trail.startOperating(player);
                        this.sendMessage(sender, "replaynk.trail.startoperating", trailName);
                    }
                } else {
                    this.sendMessage(sender, "replaynk.trail.alreadyexist", trailName);
                }
                return true;
            }
            case "remove" -> {
                if (args.length < 2) {
                    return false;
                }
                String trailName = args[1];
                var trail = Trail.removeTrail(trailName);
                if (trail != null)
                    this.sendMessage(sender, "replaynk.trail.removed", trailName);
                else
                    this.sendMessage(sender, "replaynk.trail.notfound", trailName);
                return true;
            }
            case "list" -> {
                var strBuilder = new StringBuilder();
                var trails = Trail.getTrails();
                for (var trail : trails.values()) {
                    strBuilder.append(trail.getName()).append(" ");
                }
                this.sendMessage(sender, "replaynk.command.replay.list", strBuilder.toString());
                return true;
            }
            case "play" -> {
                ArrayList<Player> targets = new ArrayList<>();
                if (args[1].equals("@s") || args[1].equals("@p")) {
                    targets.add(sender.asPlayer());
                } else if (args[1].equals("@a")) {
                    targets.addAll(Server.getInstance().getOnlinePlayers().values());
                } else {
                    Player target = Server.getInstance().getPlayer(args[1]);

                    if (target != null) {
                        targets.add(target);
                    }
                }

                if (targets.isEmpty()) {
                    this.sendMessage(sender, "replaynk.player.notfound", args[1]);
                    return false;
                }

                if (!Trail.getTrails().containsKey(args[2])) {
                    this.sendMessage(sender, "replaynk.trail.notfound", args[2]);
                    return false;
                }
                targets.forEach(target -> {
                    var trail = TrailAgent.init(target, args[2]);
                    assert trail != null;
                    trail.play(false);
                });
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void sendMessage(CommandSender sender, String key, String... params) {
        var i18n = PluginI18nManager.getI18n(ReplayNK.getInstance());
        if (i18n != null) {
            String text;
            if (sender.isPlayer()) {
                text = i18n.tr(sender.asPlayer().getLanguageCode(), key, params);
            } else {
                //TODO
                text = i18n.tr(/*Server.getInstance().getLanguageCode()*/ LangCode.zh_CN, key, params);
            }
            sender.sendMessage(text);
            return;
        }
        sender.sendMessage(Server.getInstance().getLanguage().translateString(key, params));
    }
}
