package logai

/**
  * Created by gnagar on 26/08/16.
  */
object LogAIUtils {

  def sanitizePath(path:String) = if(!path.endsWith("/")) path + "/" else path

}
