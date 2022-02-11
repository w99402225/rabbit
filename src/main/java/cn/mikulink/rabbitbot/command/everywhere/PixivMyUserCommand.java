package cn.mikulink.rabbitbot.command.everywhere;

import cn.mikulink.rabbitbot.constant.ConstantPixiv;
import cn.mikulink.rabbitbot.entity.CommandProperties;
import cn.mikulink.rabbitbot.entity.ReString;
import cn.mikulink.rabbitbot.entity.pixiv.PixivImageInfo;
import cn.mikulink.rabbitbot.exceptions.RabbitApiException;
import cn.mikulink.rabbitbot.service.PixivService;
import cn.mikulink.rabbitbot.service.RabbitBotService;
import cn.mikulink.rabbitbot.service.SetuService;
import cn.mikulink.rabbitbot.service.sys.PixivMyUserService;
import cn.mikulink.rabbitbot.service.sys.SwitchService;
import cn.mikulink.rabbitbot.sys.annotate.Command;
import cn.mikulink.rabbitbot.utils.CollectionUtil;
import cn.mikulink.rabbitbot.utils.NumberUtil;
import com.alibaba.fastjson.JSONException;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fqq
 * @date 2022/2/10
 * <p>
 * 根据指定作者列表发送图片
 */

@Command
public class PixivMyUserCommand extends BaseEveryWhereCommand {

    private static final Logger logger = LoggerFactory.getLogger(PixivMyUserCommand.class);
    //操作间隔 账号，操作时间戳
    private static Map<Long, Long> PIXIV_USER_ID_SPLIT_MAP = new HashMap<>();
    //操作间隔
    private static final Long PIXIV_USER_ID_SPLIT_TIME = 1000L * 10;
    private static final String PIXIV_USER_ID_SPLIT_ERROR = "[%s]%s秒后可以使用puid搜索";

    @Autowired
    private PixivService pixivService;
    @Autowired
    private PixivMyUserService pixivMyUserService;
    @Autowired
    private RabbitBotService rabbitBotService;
    @Autowired
    private SwitchService switchService;
    @Autowired
    private SetuService setuService;

    @Override
    public CommandProperties properties() {
        return new CommandProperties("PixivMyUserIllust", "来点涩涩");
    }


    @Override
    public Message execute(User sender, ArrayList<String> args, MessageChain messageChain, Contact subject) {
        //检查功能开关
        ReString reStringSwitch = switchService.switchCheck(sender, subject, "setu");
        if (!reStringSwitch.isSuccess()) {
            return new PlainText(reStringSwitch.getMessage());
        }

        Long userId = sender.getId();
        String userNick = sender.getNick();

        //获取指令参数
        Integer setuCount = 1;
        if (CollectionUtil.isNotEmpty(args)) {
            //第一个指令作为色图数量，最少一个，最多10个 参数不合法的时候,使用默认值
            String setuCountStr = args.get(0);
            if (NumberUtil.isNumberOnly(setuCountStr)) {
                setuCount = NumberUtil.toInt(setuCountStr);
            }
            if (setuCount <= 0) {
                setuCount = 1;
            }
            if (setuCount > 10) {
                setuCount = 10;
            }
        }

        //检查操作间隔
        ReString reString = setuService.setuTimeCheck(userId, userNick);
        if (!reString.isSuccess()) {
            return new PlainText(reString.getMessage());
        }

        //刷新操作间隔
        ConstantPixiv.SETU_PID_SPLIT_MAP.put(sender.getId(), System.currentTimeMillis());
        try {
            List<Long> pixivAuthorList = pixivMyUserService.loadList();
            int total = pixivAuthorList.size();
            for (int count = 0; count<setuCount ;count++){
                int rollAuthor = (int) ( total * Math.random());
                String theAuthor = String.valueOf(pixivAuthorList.get(rollAuthor));
                try {
                    List<PixivImageInfo> pixivImageInfoList = pixivService.getPixivIllustByUserId(theAuthor,1);
                    if (CollectionUtil.isEmpty(pixivImageInfoList)) {
                        return new PlainText(ConstantPixiv.PIXIV_MEMBER_NO_ILLUST);
                    }
                    //拼装一条发送一条
                    for (PixivImageInfo pixivImageInfo : pixivImageInfoList) {
                        pixivImageInfo.setSender(sender);
                        pixivImageInfo.setSubject(subject);
                        MessageChain tempMsg = pixivService.parsePixivImgInfoByApiInfo(pixivImageInfo);
                        subject.sendMessage(tempMsg);
                    }
                } catch (JSONException jsonEx) {
                    logger.info("PUserIdIllustCommand " + ConstantPixiv.PIXIV_MEMBER_ID_JSON_ERROR, jsonEx);
                    return new PlainText(ConstantPixiv.PIXIV_MEMBER_ID_JSON_ERROR);
                } catch (RabbitApiException rabbitEx) {
                    logger.info("PUserIdIllustCommand " + ConstantPixiv.PIXIV_MEMBER_GET_ERROR_GROUP_MESSAGE + rabbitEx.toString());
                    return new PlainText(ConstantPixiv.PIXIV_IMAGE_TIMEOUT + ":" + rabbitEx.toString());
                } catch (SocketTimeoutException stockTimeoutEx) {
                    logger.error("PUserIdIllustCommand " + ConstantPixiv.PIXIV_IMAGE_TIMEOUT + stockTimeoutEx.toString());
                    return new PlainText(ConstantPixiv.PIXIV_IMAGE_TIMEOUT);
                } catch (Exception ex) {
                    logger.error("PUserIdIllustCommand " + ConstantPixiv.PIXIV_MEMBER_GET_ERROR_GROUP_MESSAGE + ex.toString(), ex);
                    //异常后清除间隔允许再次操作
                    PIXIV_USER_ID_SPLIT_MAP.remove(sender.getId());
                    return new PlainText(ConstantPixiv.PIXIV_MEMBER_GET_ERROR_GROUP_MESSAGE);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
