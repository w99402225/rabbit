package cn.mikulink.rabbitbot.constant;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fqq
 * 存放作者列表
 */
public class ConstantPixivMyUser extends ConstantCommon{
    //作者名单
    public static List<Long> USER_GROUP = new ArrayList<>();

    public static final String ADD_SUCCESS = "作者名单添加完毕";
    public static final String REMOVE_SUCCESS = "作者名单移除完毕";
}
