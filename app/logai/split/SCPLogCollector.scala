package logai.split

import play.api.Logger

import scala.sys.process.Process

class SCPLogCollector(host: String, remoteDir: String, localDir: String, key: String) {

  //  private val sshLogin = PasswordLogin("root", SimplePasswordProducer("***"))

  //  def collect() = {
  //    SSH(host, HostConfigProvider.login2HostConfigProvider(sshLogin)) { client =>
  //      client.download(dir,localDir)
  //    }
  //  }

  def collect() = {
    Logger.info(s"Copying logs from ${remoteDir} to ${localDir}")
    val sshString = "-e ssh -i " + key
    val command = Seq("rsync", "-avz", sshString, s"root@${host}:${remoteDir}", localDir)
   //val command = s"scp -r -i ${key} root@${host}:${remoteDir} ${localDir}"
    Logger.info("Executing command : "+ command.mkString(" "))
    val rsync = Process(command)
    rsync.lineStream_!.foreach(println)
    val exitvalue = rsync.run().exitValue()
    Logger.info(s"Copy logs finished from ${remoteDir} to ${localDir} with ${exitvalue}")
    if(exitvalue != 0) {
      throw new Exception(s"Copy logs failed from ${remoteDir} to ${localDir} with ${exitvalue}")
    }
  }
}
