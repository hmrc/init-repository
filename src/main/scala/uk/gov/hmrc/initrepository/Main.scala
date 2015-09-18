package uk.gov.hmrc.initrepository

import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global

class Logger{
  def info(st:String) = println("[INFO] " + st)
  def debug(st:String) = println("[DEBUG] " + st)
}

object Main {

  val log = new Logger()

  def main(args: Array[String]) {
    val githubCredsFile  = System.getProperty("user.home") + "/.github/.credentials"
    val bintrayCredsFile = System.getProperty("user.home") + "/.bintray/.credentials"

    val githubCredsOpt  = CredentialsFinder.findGithubCredsInFile(new File(githubCredsFile).toPath)
    val bintrayCredsOpt = CredentialsFinder.findBintrayCredsInFile(new File(bintrayCredsFile).toPath)

    val github  = new Github(new GithubHttp{
      override def cred: ServiceCredentials = githubCredsOpt.get
    }, new GithubUrls())

    val bintray = new Bintray(new BintrayHttp(githubCredsOpt.get))

    val newRepoName = "domain"
    
    github.containsRepo(newRepoName) map { containsRepo =>
      if(!containsRepo){
        bintray.containsRepo(newRepoName) map { bintrayRepoExists =>
          if(!bintrayRepoExists){
            bintray.createRepo(newRepoName)
            github.createRepo(newRepoName)
          }
        }
      } else {
        new Logger().info("Repo already exists in github ")
      }
    }
  }

}
