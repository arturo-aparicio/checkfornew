@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.1')
import groovyx.net.http.RESTClient
import java.text.SimpleDateFormat
import org.artifactory.repo.Repositories;

/**
 * Intercepts a request for a remote repository, if the item is cached, 
 * it checks the remote source for changes (using a HEAD request). If
 * a change is detected, deletes the locally cached file and then proceeds
 * with the request.
 * 
 * @author Arturo Aparicio
 */

download {
    // The names of all the remote repositories to apply this plugin to
    def remoteRepoNames = ["repos", "other"]

    // Constants
    def REMOTE = "remote"
    // WARNING - SECURITY IMPLICATIONS
    // Allows users that don't have the delete permission the ability to delete
    // the file if it is going to be replaced with a newer version
    def ALLOW_DELETE_WITHOUT_USER_PERMISSION = true

    beforeDownloadRequest { request, repoPath ->
        log.debug "new beforeDownloadRequest"
        // Only intercept the download to a specific set of remote repos
        def repoName = repoPath.getRepoKey()
        if (remoteRepoNames.contains(repoName))
        {
            log.debug "Repository is in list of repos to apply plugin to."
            // Check if the repo path exists, if it does NOT, no need to
            // check for a new version.
            if (!repositories.exists(repoPath)) {
                log.debug "Path does not exist - No further processing required."
                return
            }
            // Check if the repository is a valid remote repo
            def repoConfig = repositories.getRepositoryConfiguration(repoName)
            if (repoConfig == null) {
                log.info "Error getting configuration for $repoName. \
                Check if the repository exists."
                return
            }
            if (!repoConfig.getType().equals(REMOTE)) {
                log.info "Repository $repoName must be a remote repository."
                return
            }

            // Check if there is a newer version, if there is,
            // delete the current verion
            if (newVersionExists(repoConfig, repoPath))
            {
                log.debug "Newer version exist, deleting cached artifact."
                if (ALLOW_DELETE_WITHOUT_USER_PERMISSION)
                {
                    asSystem {
                        log.debug "Performing delete as system."
                        repositories.delete(repoPath)
                    }
                } else {
                    repositories.delete(repoPath)
                }
            }
        }
    }
}

/**
 * Determines if the cached artifact is the same as that in the remote.
 * 
 * @param repoPath The repository and fragment URL combined
 * @return true if and only if the cached artifact differs from the remote
 */
private def newVersionExists(repoConfig, repoPath) {
    def remoteURL = repoConfig.getUrl()
    if (remoteURL.charAt(remoteURL.length() - 1) != '/')
        remoteURL += "/"
    def path = repoPath.getPath()

    // Comparison logic goes here
    def createdLocally = repositories.getFileInfo(repoPath).getCreated()
    def client = new RESTClient(remoteURL)
    try {
        def response = client.head path: path
        def lastModified = response.headers['Last-Modified']
        // The remote repository is not sending a Last-Modified tag
        if (lastModified == null) {
            log.debug "Last-Modified header does not exist!"
            // Change to return true if you want to retrieve it even
            // if it has not changed.
            return false
        }
        // Get a timestamp
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
        Date d = format.parse(lastModified.getValue().toString())
        long lastModifiedTimestamp = d.getTime()
        log.debug "Comparing: lastModifiedTimestamp($lastModifiedTimestamp) > createdLocally($createdLocally)"
        // The remote resource has been modified after it was originally created. 
        if (lastModifiedTimestamp > createdLocally)
            return true
    } catch( ex ) {
        log.error "Error: $ex"
        // Add error handling for a more robust implementation
    }
    return false
}