package cn.mikulink.rabbitbot.command.everywhere;

import cn.mikulink.rabbitbot.entity.CommandProperties;
import cn.mikulink.rabbitbot.service.RabbitBotService;
import cn.mikulink.rabbitbot.sys.annotate.Command;
import cn.mikulink.rabbitbot.utils.RandomUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;


/**
 * @author MikuLink
 * @date 2020/8/31 10:50
 * for the Reisen
 * <p>
 * roll数字
 */
@Command
public class RandomNumberCommand extends BaseEveryWhereCommand {
    @Autowired
    private RabbitBotService rabbitBotService;

    @Override
    public CommandProperties properties() {
        return new CommandProperties("random", "r");
    }

    @Override
    public Message execute(User sender, ArrayList<String> args, MessageChain messageChain, Contact subject) {
        //随机数 0 - 100 包含0
        int rollNum = RandomUtil.roll();
        //实际不需要0，排除掉
        if (0 == rollNum) {
            rollNum = 1;
        }

        //群员名称
        String userName = rabbitBotService.getUserName(subject, sender);
        //附加指令
        String commandParam = "";
        if (null != args && args.size() > 0) {
            commandParam = String.format("为[%s]", args.get(0));
        }

        //【群员名称】 装饰性语句 "roll="随机数
        String resultStr = String.format("[%s]%s roll=%s", userName, commandParam, rollNum);
        return new PlainText(resultStr);
    }

}
