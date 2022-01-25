package cn.mikulink.rabbitbot.command.everywhere;

import cn.mikulink.rabbitbot.constant.ConstantBlackGroup;
import cn.mikulink.rabbitbot.constant.ConstantBlackList;
import cn.mikulink.rabbitbot.constant.ConstantCommon;
import cn.mikulink.rabbitbot.entity.CommandProperties;
import cn.mikulink.rabbitbot.service.RabbitBotService;
import cn.mikulink.rabbitbot.service.sys.BlackGroupService;
import cn.mikulink.rabbitbot.sys.annotate.Command;
import cn.mikulink.rabbitbot.utils.NumberUtil;
import cn.mikulink.rabbitbot.utils.RandomUtil;
import cn.mikulink.rabbitbot.utils.StringUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Command
public class BlackGroupCommand extends BaseEveryWhereCommand{
    private static final Logger logger = LoggerFactory.getLogger(BlackGroupCommand.class);

    @Autowired
    private RabbitBotService rabbitBotService;
    @Autowired
    private BlackGroupService blackGroupService;

    @Override
    public CommandProperties properties() {
        return new CommandProperties("BlackGroup", "blackgroup");
    }

    @Override
    public Message execute(User sender, ArrayList<String> args, MessageChain messageChain, Contact subject) {
        //权限限制
        if (!rabbitBotService.isMaster(sender.getId())) {
            return new PlainText(RandomUtil.rollStrFromList(ConstantCommon.COMMAND_MASTER_ONLY));
        }

        if (null == args || args.size() == 0) {
            return new PlainText("[.blackgroup (add,remove)]");
        }

        if (args.size() < 2 || StringUtil.isEmpty(args.get(1))) {
            return new PlainText(ConstantBlackList.ADD_OR_REMOVE_BLACK_ID_USERID_EMPTY);
        }

        //二级指令
        String arg = args.get(0);
        String groupIds = args.get(1);
        switch (arg) {
            case ConstantCommon.ADD:
                //添加黑名单
                blackGroupAdd(groupIds, ConstantBlackGroup.ADD);
                return new PlainText(ConstantBlackGroup.ADD_SUCCESS);
            case ConstantCommon.REMOVE:
                //移除黑名单
                blackGroupAdd(groupIds, ConstantBlackGroup.REMOVE);
                return new PlainText(ConstantBlackGroup.REMOVE_SUCCESS);
        }
        return null;
    }

    private void blackGroupAdd(String idsStr, String action) {
        //解析出ids
        if (StringUtil.isEmpty(idsStr)) return;
        String[] ids = idsStr.split(",");
        if (ids.length <= 0) return;

        List<Long> idList = new ArrayList<>();
        for (String id : ids) {
            if (StringUtil.isEmpty(id)) continue;
            if (!NumberUtil.isNumberOnly(id)) continue;
            idList.add(NumberUtil.toLong(id));
        }
        try {
            //添加或移除黑名单
            switch (action) {
                case ConstantCommon.ADD:
                    //添加黑名单
                    idList = blackGroupService.writeFile(idList);
                    //新的黑名单添加到当前列表里
                    ConstantBlackGroup.BLACK_GROUP.addAll(idList);
                    break;
                case ConstantCommon.REMOVE:
                    //移除黑名单
                    idList = blackGroupService.removeBlack(idList);
                    //覆盖当前黑名单列表
                    ConstantBlackGroup.BLACK_GROUP = idList;
                    break;
            }
        } catch (Exception ex) {
            logger.error("黑名单业务异常,ids:{},action:{}", idsStr, action, ex);
        }
    }
}
