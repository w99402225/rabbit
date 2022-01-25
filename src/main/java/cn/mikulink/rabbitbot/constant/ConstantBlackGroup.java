package cn.mikulink.rabbitbot.constant;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fqq
 * 存放群黑名单
 */
public class ConstantBlackGroup extends ConstantCommon{
    //黑名单 针对所有功能 填写qq号即可
    public static List<Long> BLACK_GROUP = new ArrayList<>();

    public static final String ADD_OR_REMOVE_BLACK_ID_USERID_EMPTY = "订阅与取消订阅操作需要传入B站uid";
    public static final String ADD_SUCCESS = "黑名单添加完毕";
    public static final String REMOVE_SUCCESS = "黑名单移除完毕";
}
