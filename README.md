# checkfornew

## What is this?

The checkfornew	plugin is to be used with a set of Artifactory remote repositories. If a requested artifact is in a remote cache, the plugin will check the remote source for changes. In this instance it will use the md5 header, if it detects a change, it will delete the locally cached copy, forcing the request to pull the new version.


### Requirements:

Any user requesting a download should have delete permissions for the repository, otherwise the plugin cannot remove the locally cached copy. This means that you may have to give delete permission to anonymous.


### Installation:

1. Follow the plugin installation instructions here: https://www.jfrog.com/confluence/display/RTF/User+Plugins#UserPlugins-DeployingPlugins
   * Note that for Artifactory HA, the installation directory is ${CLUSTER_HOME}/ha-etc/plugins
2. The Artifactory log directory should show the plugin being loaded:
```
2016-01-28 23:00:08,029 [art-init] [INFO ] (o.a.a.p.GroovyRunnerImpl:268) - Loading script from 'checkfornew.groovy'.
```

### Logging

Note the use of log.info will not appear in the artifactory logs unless you [enable](https://www.jfrog.com/confluence/display/RTF/User+Plugins#UserPlugins-ControllingPluginLogLevel) it. Another option would be to use info.error, which will write to the logs even without changing logback.xml.

### Making changes
This versions uses the header Content-MD5 but that may not be available or you may prefer to use last-updated or some other mechanism. For this the method `newVersionExists` needs to be updated. 
