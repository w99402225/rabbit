package cn.mikulink.rabbitbot.command.everywhere;

import cn.mikulink.rabbitbot.constant.ConstantBlackGroup;
import cn.mikulink.rabbitbot.constant.ConstantCommon;
import cn.mikulink.rabbitbot.constant.ConstantPixivMyUser;
import cn.mikulink.rabbitbot.entity.CommandProperties;
import cn.mikulink.rabbitbot.service.RabbitBotService;
import cn.mikulink.rabbitbot.service.sys.PixivMyUserService;
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

/**
 * @author fqq
 */
@Command
public class PixivAuthorListCommand extends BaseEveryWhereCommand{
    private static final Logger logger = LoggerFactory.getLogger(BlackGroupCommand.class);

    @Autowired
    private RabbitBotService rabbitBotService;
    @Autowired
    private PixivMyUserService pixivMyUserService;

    @Override
    public CommandProperties properties() {
        return new CommandProperties("AuthorList", "pixivauthor");
    }

    @Override
    public Message execute(User sender, ArrayList<String> args, MessageChain messageChain, Contact subject) {
        //权限限制
        if (!rabbitBotService.isMaster(sender.getId())) {
            return new PlainText(RandomUtil.rollStrFromList(ConstantCommon.COMMAND_MASTER_ONLY));
        }

        if (null == args || args.size() == 0) {
            return new PlainText("[.pixivauthor (add,remove)]");
        }

        //二级指令
        String arg = args.get(0);
        String groupIds = args.get(1);
        switch (arg) {
            case ConstantCommon.ADD:
                //添加黑名单
                pixivAuthorAdd(groupIds, ConstantPixivMyUser.ADD);
                return new PlainText(ConstantPixivMyUser.ADD_SUCCESS);
            case ConstantCommon.REMOVE:
                //移除黑名单
                pixivAuthorAdd(groupIds, ConstantPixivMyUser.REMOVE);
                return new PlainText(ConstantPixivMyUser.REMOVE_SUCCESS);
        }
        return null;
    }

    private void pixivAuthorAdd(String idsStr, String action) {
        //解析出ids
        if (StringUtil.isEmpty(idsStr)) {
            return;
        }
        String[] ids = idsStr.split(",");
        if (ids.length <= 0) {
            return;
        }

        List<Long> idList = new ArrayList<>();
        for (String id : ids) {
            if (StringUtil.isEmpty(id)) {
                continue;
            }
            if (!NumberUtil.isNumberOnly(id)) {
                continue;
            }
            idList.add(NumberUtil.toLong(id));
        }
        try {
            //添加或移除作者名单
            switch (action) {
                case ConstantCommon.ADD:
                    //添加作者名单
                    idList = pixivMyUserService.writeFile(idList);
                    //新的作者名单添加到当前列表里
                    ConstantPixivMyUser.USER_GROUP.addAll(idList);
                    break;
                case ConstantCommon.REMOVE:
                    //移除作者名单
                    idList = pixivMyUserService.removeList(idList);
                    //覆盖当前作者名单列表
                    ConstantPixivMyUser.USER_GROUP = idList;
                    break;
            }
        } catch (Exception ex) {
            logger.error("作者名单业务异常,ids:{},action:{}", idsStr, action, ex);
        }
    }
}
