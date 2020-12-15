package cn.mikulink.command.everywhere;

import cn.mikulink.constant.ConstantCommon;
import cn.mikulink.constant.ConstantImage;
import cn.mikulink.service.PixivBugService;
import cn.mikulink.service.PixivService;
import cn.mikulink.utils.NumberUtil;
import cn.mikulink.utils.StringUtil;
import cn.mikulink.entity.CommandProperties;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;


/**
 * @author MikuLink
 * @date 2020/8/31 10:50
 * for the Reisen
 * <p>
 * 根据pixiv图片id搜索图片
 */
@Component
public class PidCommand extends BaseEveryWhereCommand {
    private static final Logger logger = LoggerFactory.getLogger(PidCommand.class);

    @Autowired
    private PixivService pixivService;
    @Autowired
    private PixivBugService pixivBugService;

    @Override
    public CommandProperties properties() {
        return new CommandProperties("PixivImageId", "pid");
    }

    @Override
    public Message execute(User sender, ArrayList<String> args, MessageChain messageChain, Contact subject) {
        if (null == args || args.size() == 0) {
            return new PlainText(ConstantImage.PIXIV_IMAGE_ID_IS_EMPTY);
        }
        //基本输入校验
        String pid = args.get(0);
        if (StringUtil.isEmpty(pid)) {
            return new PlainText(ConstantImage.PIXIV_IMAGE_ID_IS_EMPTY);
        }
        if (!NumberUtil.isNumberOnly(pid)) {
            return new PlainText(ConstantImage.PIXIV_IMAGE_ID_IS_NUMBER_ONLY);
        }

        try {
            MessageChain resultChain = null;
            //是否走爬虫
            String pixiv_config_use_api = ConstantCommon.common_config.get(ConstantImage.PIXIV_CONFIG_USE_API);
            if (ConstantImage.OFF.equalsIgnoreCase(pixiv_config_use_api)) {
//                resultChain = pixivBugService.searchPixivImgById(NumberUtil.toLong(pid), subject);
            } else {
                resultChain = pixivService.searchPixivImgById(NumberUtil.toLong(pid), subject);
            }

            return resultChain;
        } catch (FileNotFoundException fileNotFoundEx) {
            logger.warn(ConstantImage.PIXIV_IMAGE_DELETE + fileNotFoundEx.toString());
            return new PlainText(ConstantImage.PIXIV_IMAGE_DELETE);
        } catch (SocketTimeoutException stockTimeoutEx) {
            logger.warn(ConstantImage.PIXIV_IMAGE_TIMEOUT + stockTimeoutEx.toString());
            return new PlainText(ConstantImage.PIXIV_IMAGE_TIMEOUT);
        } catch (Exception ex) {
            logger.error(ConstantImage.PIXIV_ID_GET_ERROR_GROUP_MESSAGE + ex.toString(), ex);
            return new PlainText(ConstantImage.PIXIV_ID_GET_ERROR_GROUP_MESSAGE);
        }
    }

}
