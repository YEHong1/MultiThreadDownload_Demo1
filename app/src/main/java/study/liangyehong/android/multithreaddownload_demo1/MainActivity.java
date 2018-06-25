package study.liangyehong.android.multithreaddownload_demo1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class MainActivity extends Activity {

    private EditText et_path;
    private EditText et_threadCount;
    private LinearLayout ll_pb_layout;
    private String path;
    private static int runningThread;  //代表当前正在运行的线程
    private int threadCount;
    private List<ProgressBar> pbLists; //用来存进度条的引用
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // [1]找到我们关心的控件

        et_path = (EditText) findViewById(R.id.et_path);
        et_threadCount = (EditText) findViewById(R.id.et_threadCount);
        ll_pb_layout = (LinearLayout) findViewById(R.id.ll_pb);


        //[2]添加一个集合 用来存进度条的引用
        pbLists = new ArrayList<ProgressBar>();

    }

    //点击按钮实现下载的逻辑
    public void click(View v){

        //[2]获取下载的路径

        path = et_path.getText().toString().trim();

        //[3]获取线程的数量
        String threadCountt  = et_threadCount.getText().toString().trim();
        //[3.0]先移除进度条 在添加
        ll_pb_layout.removeAllViews();

        threadCount = Integer.parseInt(threadCountt);
        pbLists.clear();
        for (int i = 0; i < threadCount; i++) {

            //[3.1]把我定义的item布局转换成一个view对象
            ProgressBar pbView = (ProgressBar) View.inflate(getApplicationContext(), R.layout.item, null);

            //[3.2]把pbView 添加到集合中
            pbLists.add(pbView);

            //[4]动态的添加进度条
            ll_pb_layout.addView(pbView);

        }


        //[5]开始移植   联网 获取文件长度
        new Thread(){public void run() {

            //[一 ☆☆☆☆]获取服务器文件的大小   要计算每个线程下载的开始位置和结束位置

            try {

                //(1) 创建一个url对象 参数就是网址
                URL url = new URL(path);
                //(2)获取HttpURLConnection 链接对象
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                //(3)设置参数  发送get请求
                conn.setRequestMethod("GET"); //默认请求 就是get  要大写
                //(4)设置链接网络的超时时间
                conn.setConnectTimeout(5000);
                //(5)获取服务器返回的状态码
                int code = conn.getResponseCode(); //200  代表获取服务器资源全部成功  206请求部分资源
                if (code == 200) {

                    //(6)获取服务器文件的大小
                    int length = conn.getContentLength();

                    //[6.1]把 线程的数量赋值给正在运行的线程
                    runningThread = threadCount;


                    System.out.println("length:"+length);

                    //[二☆☆☆☆ ] 创建一个大小和服务器一模一样的文件 目的提前把空间申请出来
                    RandomAccessFile rafAccessFile = new RandomAccessFile(getFilename(path), "rw");
                    rafAccessFile.setLength(length);

                    //(7)算出每个线程下载的大小
                    int blockSize = length /threadCount;

                    //[三☆☆☆☆  计算每个线程下载的开始位置和结束位置 ]
                    for (int i = 0; i < threadCount; i++) {
                        int startIndex = i * blockSize;   //每个线程下载的开始位置
                        int endIndex = (i+1)*blockSize - 1;
                        //特殊情况 就是最后一个线程
                        if (i == threadCount - 1) {
                            //说明是最后一个线程
                            endIndex = length - 1;

                        }

                        System.out.println("线程id:::"+i + "理论下载的位置"+":"+startIndex+"-----"+endIndex);

                        //四 开启线程去服务器下载文件
                        DownLoadThread downLoadThread = new DownLoadThread(startIndex, endIndex, i);
                        downLoadThread.start();

                    }



                }

            } catch (Exception e) {
                e.printStackTrace();
            }


        };}.start();




    }


    //定义线程去服务器下载文件
    private  class DownLoadThread extends Thread{
        //通过构造方法把每个线程下载的开始位置和结束位置传递进来

        private int startIndex;
        private int endIndex;
        private int threadId;

        private int PbMaxSize; //代表当前线程下载的最大值
        //如果中断过  获取上次下载的位置
        private int pblastPostion;

        public DownLoadThread(int startIndex,int endIndex,int threadId){
            this.startIndex = startIndex;
            this.endIndex  = endIndex;
            this.threadId = threadId;
        }

        @Override
        public void run() {
            //四  实现去服务器下载文件的逻辑

            try {

                //(0)计算当前进度条的最大值
                PbMaxSize = endIndex - startIndex;
                //(1) 创建一个url对象 参数就是网址
                URL url = new URL(path);
                //(2)获取HttpURLConnection 链接对象
                HttpURLConnection conn = (HttpURLConnection) url
                        .openConnection();
                //(3)设置参数  发送get请求
                conn.setRequestMethod("GET"); //默认请求 就是get  要大写
                //(4)设置链接网络的超时时间
                conn.setConnectTimeout(5000);


                //[4.0]如果中间断过  继续上次的位置 继续下载   从文件中读取上次下载的位置

                File file =new File(getFilename(path)+threadId+".txt");
                if (file.exists() && file.length()>0) {
                    FileInputStream fis = new  FileInputStream(file);
                    BufferedReader bufr = new  BufferedReader(new InputStreamReader(fis));
                    String lastPositionn = bufr.readLine();  //读取出来的内容就是上一次下载的位置
                    int lastPosition = Integer.parseInt(lastPositionn);

                    //[4.0]给我们定义的进度条条位置 赋值
                    pblastPostion = lastPosition - startIndex;

                    //[4.0.1]要改变一下 startIndex 位置
                    startIndex = lastPosition + 1;



                    System.out.println("线程id::"+threadId + "真实下载的位置"+":"+startIndex+"-----"+endIndex);

                    fis.close(); //关闭流
                }

                //[4.1]设置一个请求头Range (作用告诉服务器每个线程下载的开始位置和结束位置)
                conn.setRequestProperty("Range", "bytes="+startIndex+"-"+endIndex);

                //(5)获取服务器返回的状态码
                int code = conn.getResponseCode(); //200  代表获取服务器资源全部成功  206请求部分资源 成功
                if (code == 206) {
                    //[6]创建随机读写文件对象
                    RandomAccessFile raf = new RandomAccessFile(getFilename(path), "rw");
                    //[6]每个线程要从自己的位置开始写
                    raf.seek(startIndex);

                    InputStream in = conn.getInputStream(); //存的是feiq.exe

                    //[7]把数据写到文件中
                    int len = -1;
                    byte[] buffer = new byte[1024*1024];//1Mb

                    int total = 0; //代表当前线程下载的大小

                    while((len = in.read(buffer))!=-1){
                        raf.write(buffer, 0, len);

                        total +=len;
                        //[8]实现断点续传 就是把当前线程下载的位置 给存起来 下次再下载的时候 就是按照上次下载的位置继续下载 就可以了
                        int currentThreadPosition =  startIndex + total;  //比如就存到一个普通的.txt文本中

                        //[9]用来存当前线程下载的位置
                        RandomAccessFile raff = new RandomAccessFile(getFilename(path)+threadId+".txt", "rwd");
                        raff.write(String.valueOf(currentThreadPosition).getBytes());
                        raff.close();

                        //[10]设置一下当前进度条的最大值 和 当前进度
                        pbLists.get(threadId).setMax(PbMaxSize);//设置进度条的最大值
                        pbLists.get(threadId).setProgress(pblastPostion+total);//设置当前进度条的当前进度



                    }
                    raf.close();//关闭流  释放资源

                    System.out.println("线程id:"+threadId + "---下载完毕了");

                    //[10]把.txt文件删除  每个线程具体什么时候下载完毕了 我们不知道

                    synchronized (DownLoadThread.class) {
                        runningThread--;
                        if (runningThread == 0) {
                            //说明所有的线程都执行完毕了 就把.txt文件删除
                            for (int i = 0; i < threadCount; i++) {
                                File  delteFile = new File(getFilename(path)+i+".txt");
                                delteFile.delete();
                            }




                        }
                    }





                }

            } catch (Exception e) {
                e.printStackTrace();
            }



        }
    }



    //获取文件的名字    "http://10.10.11.13/文件名";
    public  String getFilename(String path){

        int start =  path.lastIndexOf("/")+1;
        String substring = path.substring(start);

        String fileName = Environment.getExternalStorageDirectory().getPath()+"/"+substring;
        return fileName;
    }





}
