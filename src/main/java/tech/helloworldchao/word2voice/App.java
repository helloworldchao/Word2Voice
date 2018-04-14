package tech.helloworldchao.word2voice;

import com.baidu.aip.speech.AipSpeech;
import com.baidu.aip.speech.TtsResponse;
import com.baidu.aip.util.Util;
import org.json.JSONObject;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;

public class App {

    public static void main(String[] args) {
        System.out.println("文字转语音工具v1.0\n" +
                "作者:helloworldchao\n" +
                "联系邮箱:helloworldchao@outlook.com\n");

        if (args.length == 0 || args[0] == null || "".equals(args[0]) || "-h".equals(args[0])) {
            System.out.println("请输入参数后使用（带*号为必填项）：\n" +
                    "1、文件路径*：相对路径或绝对路径皆可\n" +
                    "2、语速：语速，取值0-9，默认为5中语速\n" +
                    "3、音调：音调，取值0-9，默认为5中语调\n" +
                    "4、音量：音量，取值0-15，默认为5中音量\n" +
                    "5、发音人：发音人选择, 0为女声，1为男声，3为情感合成-度逍遥，4为情感合成-度丫丫，默认为3\n" +
                    "6、语言：文字语言（zh/en），默认为zh\n");
            return;
        }

        try {
            for (int i = 0; i < args.length; i++) {
                if (i == 1) SPEED = Integer.parseInt(args[i]);
                if (i == 2) PIT = Integer.parseInt(args[i]);
                if (i == 3) VOL = Integer.parseInt(args[i]);
                if (i == 4) PER = Integer.parseInt(args[i]);
                if (i == 5) LANG = args[i];
            }
        } catch (Exception e) {
            System.out.println("Error:参数填写错误，请按照帮助填写，输入【-h】获取帮助");
        }

        //加载配置文件
        Properties config = new Properties();
        try {
            config.load(new FileInputStream("config.properties"));
        } catch (Exception e) {
            System.out.println("Error:加载配置失败，请确认配置文件【config.properties】已创建");
            return;
        }

        try {
            // 读取文件内容
            StringBuilder content = readFile(args[0]);

            // 初始化音频文件
            String fileName = findFileName(args[0]);
            String outputFileName = fileName + ".mp3";
            Util.writeBytesToFileSystem(new byte[0], outputFileName);

            // 开始文字->音频的转换
            AipSpeech client = new AipSpeech(config.getProperty("APP_ID"), config.getProperty("API_KEY"), config.getProperty("SECRET_KEY"));
            float total = content.toString().length();
            int limit = 500;
            while (content.toString().length() > 0) {
                String transContent;
                if (content.toString().length() > limit) {
                    transContent = content.toString().substring(0, limit);
                    content.delete(0, limit);
                } else {
                    transContent = content.toString().substring(0, content.toString().length());
                    content.delete(0, content.toString().length());
                }
                trans(client, transContent, outputFileName);

                System.out.print("\r转换进度：" + (100 - (content.toString().length() / total) * 100) + " %");
            }
            System.out.println("\n转换成功！输出文件：" +  outputFileName);
        } catch (TransException e) {
            System.out.println("\nError:语音接口转换失败，请检查配置内容是否正确");
        } catch (FileNotFoundException e) {
            System.out.println("\nError:无法加载源文件，请确认文件路径正确");
        } catch (Exception e) {
            System.out.println("\nError:发生未知错误，转换失败");
        }
    }

    /**
     * 根据路径取出文件名
     * @param path 路径
     * @return 文件名
     */
    private static String findFileName(String path) {
        path = path.replaceAll("/", "\\");
        path = path.substring(path.lastIndexOf("\\") + 1);
        String[] fileNameArr = path.split("\\.");
        if (fileNameArr.length > 0) {
            return fileNameArr[0];
        }
        return "";
    }

    /**
     * 读取文件
     * @param path 文件路径
     * @return 全部文件内容
     * @throws IOException IO读写错误
     */
    private static StringBuilder readFile(String path) throws IOException {
        // 加载文件
        File filename = new File(path);
        InputStreamReader reader = new InputStreamReader(new FileInputStream(filename), "UTF8");
        BufferedReader br = new BufferedReader(reader);

        // 读取所有文件内容
        String line = br.readLine();
        StringBuilder content = new StringBuilder();
        while (line != null) {
            if ("".equals(line)) { // 如果内容为空则跳过
                line = br.readLine(); // 一次读入一行数据
                continue;
            }
            content.append(line);
            line = br.readLine(); // 一次读入一行数据
        }

        // 关闭文件
        reader.close();
        br.close();

        return content;
    }

    /**
     * 整合两个数组
     * @param data1 数组一
     * @param data2 数组二
     * @return 新的数组
     */
    private static byte[] addBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;
    }

    private static String LANG = "zh"; // 语言选择,填写zh, en
    private static int SPEED = 5; // 语速，取值0-9，默认为5中语速
    private static int PIT = 5; // 音调，取值0-9，默认为5中语调
    private static int VOL = 5; // 音量，取值0-15，默认为5中音量
    private static int PER = 3; // 发音人选择, 0为女声，1为男声，3为情感合成-度逍遥，4为情感合成-度丫丫，默认为普通女

    /**
     * 文字到语音转换函数
     * @param client 百度语音合成辅助类
     * @param msg 需要转换的文字
     * @param outputFileName 输出的语音文件名称
     * @throws IOException IO读写错误
     */
    private static void trans(AipSpeech client, String msg, String outputFileName) throws TransException, IOException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("spd", SPEED);
        map.put("pit", PIT);
        map.put("vol", VOL);
        map.put("per", PER);

        // 调用接口
        TtsResponse response = client.synthesis(msg, LANG, 1, map);

        byte[] data = response.getData();
        JSONObject result = response.getResult();
        if (result != null) {
            throw new TransException();
        }
        if (data != null) {
            byte[] oldData = Util.readFileByBytes(outputFileName);
            Util.writeBytesToFileSystem(addBytes(oldData, data), outputFileName);
        }
    }
}
