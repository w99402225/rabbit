package cn.mikulink.rabbitbot.service.sys;

import cn.mikulink.rabbitbot.constant.ConstantBlackGroup;
import cn.mikulink.rabbitbot.constant.ConstantPixivMyUser;
import cn.mikulink.rabbitbot.utils.FileUtil;
import cn.mikulink.rabbitbot.utils.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * @author fqq
 */
@Service
public class PixivMyUserService {
    private Logger logger = LoggerFactory.getLogger(PixivMyUserService.class);

    @Value("${file.path.config:}")
    private String configPath;

    /**
     * 获取配置文件路径
     */
    public String getFilePath() {
        return configPath + File.separator + "pixiv_userid";
    }

    public void loadFile() {
        try {
            ConstantPixivMyUser.USER_GROUP = this.loadList();
        } catch (Exception ex) {
            logger.error("作者信息读取失败", ex);
        }
    }

    public List<Long> loadList() throws IOException {
        File pixivListFile = FileUtil.fileCheck(this.getFilePath());

        //初始化集合
        List<Long> tempList = new ArrayList<>();

        //创建读取器
        BufferedReader reader = new BufferedReader(new FileReader(pixivListFile));
        //逐行读取文件
        String AuthorPid = null;
        while ((AuthorPid = reader.readLine()) != null) {
            //过滤掉空行
            if (AuthorPid.length() <= 0) {
                continue;
            }
            //过滤掉非数字的异常数据
            if (!NumberUtil.isNumberOnly(AuthorPid)) {
                continue;
            }
            tempList.add(NumberUtil.toLong(AuthorPid));
        }
        //关闭读取器
        reader.close();

        return tempList;
    }

    /**
     * 对文件写入内容
     *
     * @throws IOException 读写异常
     */
    public List<Long> writeFile(List<Long> pids) throws IOException {

        //覆写原本配置
        //先读取出所有id，判重后直接覆写文件
        List<Long> pixivAuthorList = loadList();

        //过滤重复
        List<Long> tempNewPid = new ArrayList<>();
        for (Long pidStr : pids) {
            if (pixivAuthorList.contains(pidStr)) {
                continue;
            }
            pixivAuthorList.add(pidStr);
            tempNewPid.add(pidStr);
        }
        //创建写入流
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.getFilePath(), false)));
        for (Long qid : pixivAuthorList) {
            out.write("\r\n" + qid);
        }
        //关闭写入流
        out.close();

        //返回删选后的pid列表
        return tempNewPid;
    }

    /**
     * 移除文件内内容
     *
     * @throws IOException 读写异常
     */
    public List<Long> removeList(List<Long> pids) throws IOException {

        //覆写原本配置
        //先读取出所有id，判重后直接覆写文件
        List<Long> pixivAuthorId = loadList();

        //过滤重复
        for (Long gid : pids) {
            if (!pixivAuthorId.contains(gid)) {
                continue;
            }
            pixivAuthorId.remove(gid);
        }
        //创建写入流
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.getFilePath(), false)));
        for (Long gid : pixivAuthorId) {
            out.write("\r\n" + gid);
        }
        //关闭写入流
        out.close();

        return pixivAuthorId;
    }
}
