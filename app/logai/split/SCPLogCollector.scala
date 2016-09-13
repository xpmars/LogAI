package logai.split

import com.decodified.scalassh.{HostConfigProvider, PasswordLogin, SSH, SimplePasswordProducer}
import model.SplitJob

/**
  * Created by gnagar on 07/09/16.
  */
class SCPLogCollector(split: SplitJob, localDir: String) {

  private val dir = s"/***/${split.dir}"
  private val host = "****"
  private val sshLogin = PasswordLogin("root", SimplePasswordProducer("***"))

  def collect() = {
    SSH(host, HostConfigProvider.login2HostConfigProvider(sshLogin)) { client =>
      client.download(dir,localDir)
    }
  }

//  def collect() = {
//    val rsync = Process(Seq("rsync","-avzh", "--progress","--password-file=/Users/gnagar/work/rsync_pass",s"root@${host}:${dir}", localDir)) //#< new ByteArrayInputStream("".getBytes("UTF-8"))
//    rsync.lineStream_!.foreach(println)
//  }
}
