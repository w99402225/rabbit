package cn.mikulink.rabbitbot.event;


import cn.mikulink.rabbitbot.command.*;
import cn.mikulink.rabbitbot.constant.ConstantBlackGroup;
import cn.mikulink.rabbitbot.constant.ConstantBlackList;
import cn.mikulink.rabbitbot.constant.ConstantImage;
import cn.mikulink.rabbitbot.constant.ConstantPixiv;
import cn.mikulink.rabbitbot.entity.apirequest.saucenao.SaucenaoSearchInfoResult;
import cn.mikulink.rabbitbot.entity.pixiv.PixivImageInfo;
import cn.mikulink.rabbitbot.exceptions.RabbitException;
import cn.mikulink.rabbitbot.service.DanbooruService;
import cn.mikulink.rabbitbot.service.ImageService;
import cn.mikulink.rabbitbot.service.KeyWordService;
import cn.mikulink.rabbitbot.service.PixivService;
import cn.mikulink.rabbitbot.utils.StringUtil;
import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.contact.*;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.ListeningStatus;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.event.events.TempMessageEvent;
import net.mamoe.mirai.internal.message.OnlineImage;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 消息事件处理, 不同于其他事件, 消息事件中进一步封装了指令
 * @author: MikuLink
 * @date: 2020/12/14 17:19
 **/
@Component
public class MessageEvents extends SimpleListenerHost {
    private static final Logger logger = LoggerFactory.getLogger(MessageEvents.class);

    //操作间隔 账号，操作时间戳
    public static Map<Long, Long> PIXIV_SEARCH_SPLIT_MAP = new HashMap<>();
    @Autowired
    private CommandConfig commandConfig;
    @Autowired
    private KeyWordService keyWordService;
    @Autowired
    private ImageService imageService;
    @Autowired
    private PixivService pixivService;
    @Autowired
    private DanbooruService danbooruService;


    @Override
    public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception) {
        logger.error("RecallEvent Error:{}", exception.getMessage());
    }


    /**
     * 所有消息处理
     *
     * @param event 消息事件
     * @return 监听状态 详见 ListeningStatus
     * @throws Exception 可以抛出任何异常, 将在 handleException 处理
     */
    @NotNull
    @EventHandler
    public ListeningStatus onMessage(@NotNull MessageEvent event) throws Exception {
        //黑名单群，忽略一切指令
        if (event.getSubject() instanceof Group){
            if (ConstantBlackGroup.BLACK_GROUP.contains(event.getSubject().getId())) {
                return ListeningStatus.LISTENING;
            }
        }
        User sender = event.getSender();
        String oriMsg = event.getMessage().contentToString();
        logger.info("{接收到其他消息} userId:{},userNick:{},msg:{}", sender.getId(), sender.getNick(), event.getMessage().toString());

        //黑名单，用来防止和其他机器人死循环响应，或者屏蔽恶意人员
        if (ConstantBlackList.BLACK_LIST.contains(sender.getId())) {
            return ListeningStatus.LISTENING;
        }

        //是否指令模式
        if (!commandConfig.isCommand(oriMsg)) {
            // 非指令处理其他业务
            ArrayList<String> argsNoCommand = getArgsNoCommand(oriMsg);
            if (argsNoCommand.get(0).equals("搜图")){
                PIXIV_SEARCH_SPLIT_MAP.put(sender.getId(), System.currentTimeMillis());
                event.getSubject().sendMessage(new PlainText("快把涩图发我（）"));
            }
            if (argsNoCommand.get(0).equals("[图片]")){
                if (PIXIV_SEARCH_SPLIT_MAP.containsKey(sender.getId())){
                    if ((System.currentTimeMillis()-PIXIV_SEARCH_SPLIT_MAP.get(sender.getId()))<600*1000){
                        event.getSubject().sendMessage(new PlainText("["+sender.getNick()+"] 在搜了在搜了"));
                        Message result = search(sender, getArgs(oriMsg), event.getMessage(), event.getSubject());
                        if (result != null) {
                            event.getSubject().sendMessage(result);
                        }
                    }
                    PIXIV_SEARCH_SPLIT_MAP.remove(sender.getId());
                }
            }
            return ListeningStatus.LISTENING;
        }
        EverywhereCommand command = (EverywhereCommand) commandConfig.getCommand(oriMsg, CommandConfig.everywhereCommands);

        if (command == null) {
            return ListeningStatus.LISTENING;
        }
        //执行指令并回复结果
        Message result = command.execute(sender, getArgs(oriMsg), event.getMessage(), event.getSubject());
        if (result != null) {
            event.getSubject().sendMessage(result);
        }

        return ListeningStatus.LISTENING; // 表示继续监听事件
    }


    /**
     * 好友私聊消息事件处理
     *
     * @param event 消息事件
     * @return 监听状态 详见 ListeningStatus
     * @throws Exception 可以抛出任何异常, 将在 handleException 处理
     */
    @NotNull
    @EventHandler
    public ListeningStatus onFriendMessage(@NotNull FriendMessageEvent event) throws Exception {
        Friend sender = event.getSender();
        String oriMsg = event.getMessage().contentToString();

        logger.info("{接收到好友消息} userId:{},userNick:{},msg:{}", sender.getId(), sender.getNick(), event.getMessage().toString());

        //黑名单，用来防止和其他机器人死循环响应，或者屏蔽恶意人员
        if (ConstantBlackList.BLACK_LIST.contains(sender.getId())) {
            return ListeningStatus.LISTENING;
        }

        //是否指令模式
        if (!commandConfig.isCommand(oriMsg)) {
            return ListeningStatus.LISTENING;
        }
        FriendCommand command = (FriendCommand) commandConfig.getCommand(oriMsg, commandConfig.friendCommands);
        if (command == null) {
            return ListeningStatus.LISTENING;
        }
        //执行指令并回复结果
        Message result = command.execute(sender, getArgs(oriMsg), event.getMessage(), event.getSubject());
        if (result != null) {
            event.getSubject().sendMessage(result);
        }
        //事件拦截 防止everywhere消息事件再次处理
        event.intercept();

        return ListeningStatus.LISTENING;
    }


    /**
     * 群聊消息事件处理
     *
     * @param event 消息事件
     * @return 监听状态 详见 ListeningStatus
     * @throws Exception 可以抛出任何异常, 将在 handleException 处理
     */
    @NotNull
    @EventHandler
    public ListeningStatus onGroupMessage(@NotNull GroupMessageEvent event) throws Exception {
        Member sender = event.getSender();
        String oriMsg = event.getMessage().contentToString();

        if (ConstantBlackGroup.BLACK_GROUP.contains(sender.getGroup().getId())) {
            return ListeningStatus.LISTENING;
        }
        logger.info("{接收到群消息} groupId:{},userNick:{},userId:{},msg:%{},groupName:{},userCard:{}",
                event.getGroup().getId(), sender.getNick(), sender.getId(), event.getMessage().toString(), event.getGroup().getName(), event.getSender().getNameCard());

        //黑名单，用来防止和其他机器人死循环响应，或者屏蔽恶意人员
        if (ConstantBlackList.BLACK_LIST.contains(sender.getId())) {
            return ListeningStatus.LISTENING;
        }
        if (ConstantBlackGroup.BLACK_GROUP.contains(sender.getGroup().getId())) {
            return ListeningStatus.LISTENING;
        }


        //是否指令模式
        if (!commandConfig.isCommand(oriMsg)) {
            // 非指令处理其他业务
            //关键词响应
            keyWordService.keyWordMatchGroup(event);
            return ListeningStatus.LISTENING;
        }
        GroupCommand command = (GroupCommand) commandConfig.getCommand(oriMsg, CommandConfig.groupCommands);
        if (command == null) {
            return ListeningStatus.LISTENING;
        }

        //指令参数
        ArrayList<String> args = getArgs(oriMsg);

        //判断权限
        Message result = command.permissionCheck(sender, args, event.getMessage(), event.getSubject());
        if (null != result) {
            event.getSubject().sendMessage(result);
        } else {
            //执行指令并回复结果
            result = command.execute(sender, args, event.getMessage(), event.getSubject());
            if (result != null) {
                event.getSubject().sendMessage(result);
            }
        }
        //事件拦截 防止公共消息事件再次处理
        event.intercept();

        return ListeningStatus.LISTENING;
    }

    /**
     * 群临时消息事件处理
     *
     * @param event 消息事件
     * @return 监听状态 详见 ListeningStatus
     * @throws Exception 可以抛出任何异常, 将在 handleException 处理
     */
    @NotNull
    @EventHandler
    public ListeningStatus onTempMessage(@NotNull TempMessageEvent event) throws Exception {
        Member sender = event.getSender();

        String oriMsg = event.getMessage().contentToString();

        logger.info("{接收到临时消息} userId:{},userNick:{},msg:{}", sender.getId(), sender.getNick(), event.getMessage().toString());

        //黑名单，用来防止和其他机器人死循环响应，或者屏蔽恶意人员
        if (ConstantBlackList.BLACK_LIST.contains(sender.getId())) {
            return ListeningStatus.LISTENING;
        }

        //是否指令模式
        if (!commandConfig.isCommand(oriMsg)) {
            return ListeningStatus.LISTENING;
        }

        TempMessageCommand command = (TempMessageCommand) commandConfig.getCommand(oriMsg, CommandConfig.tempMsgCommands);
        if (command == null) {
            return ListeningStatus.LISTENING;
        }
        //执行指令并回复结果
        Message result = command.execute(sender, getArgs(oriMsg), event.getMessage(), sender);
        if (result != null) {
            event.getSubject().sendMessage(result);
        }
        //事件拦截 防止公共消息事件再次处理
        event.intercept();

        return ListeningStatus.LISTENING;
    }

    /**
     * 从消息体中获得 用空格分割的参数
     *
     * @param msg 消息
     * @return 分割出来的参数
     */
    private ArrayList<String> getArgs(String msg) {
        String[] args = msg.trim().split(" ");
        ArrayList<String> list = new ArrayList<>();
        for (String arg : args) {
            if (StringUtil.isNotEmpty(arg)) {
                list.add(arg);
            }
        }
        list.remove(0);
        return list;
    }

    /**
     * 从消息体中获得 用空格分割的参数
     *
     * @param msg 消息
     * @return 分割出来的参数
     */
    private ArrayList<String> getArgsNoCommand(String msg) {
        String[] args = msg.trim().split(" ");
        ArrayList<String> list = new ArrayList<>();
        for (String arg : args) {
            if (StringUtil.isNotEmpty(arg)) {
                list.add(arg);
            }
        }
        return list;
    }

    private Message search(User sender, ArrayList<String> args, MessageChain messageChain, Contact subject){
        String imgUrl = ((OnlineImage) messageChain.get(1)).getOriginUrl();

        if (StringUtil.isEmpty(imgUrl)) {
            return new PlainText(ConstantImage.IMAGE_SEARCH_IMAGE_URL_PARSE_FAIL);
        }
        try {
            SaucenaoSearchInfoResult searchResult = imageService.searchImgFromSaucenao(imgUrl);
            if (null == searchResult) {
                //没有符合条件的图片，识图失败
                return new PlainText(ConstantImage.SAUCENAO_SEARCH_FAIL_PARAM);
            }

            //获取信息，并返回结果
            if (5 == searchResult.getHeader().getIndex_id()) {
                //pixiv
                PixivImageInfo pixivImageInfo = pixivService.getPixivImgInfoById((long) searchResult.getData().getPixiv_id());
                pixivImageInfo.setSender(sender);
                pixivImageInfo.setSubject(subject);
                return pixivService.parsePixivImgInfoByApiInfo(pixivImageInfo, searchResult.getHeader().getSimilarity());
            } else {
                //Danbooru
                return danbooruService.parseDanbooruImgRequest(searchResult);
            }
        } catch (RabbitException rabEx) {
            //业务异常，日志吃掉
            return new PlainText(rabEx.getMessage());
        } catch (FileNotFoundException fileNotFoundEx) {
            logger.warn(ConstantPixiv.PIXIV_IMAGE_DELETE + fileNotFoundEx.toString());
            return new PlainText(ConstantPixiv.PIXIV_IMAGE_DELETE);
        } catch (SocketTimeoutException timeoutException) {
            logger.error(ConstantImage.IMAGE_GET_TIMEOUT_ERROR + timeoutException.toString(), timeoutException);
            return new PlainText(ConstantImage.IMAGE_GET_TIMEOUT_ERROR);
        } catch (Exception ex) {
            logger.error(ConstantImage.IMAGE_GET_ERROR + ex.toString(), ex);
            return new PlainText(ConstantImage.IMAGE_GET_ERROR);
        }
    }
}
