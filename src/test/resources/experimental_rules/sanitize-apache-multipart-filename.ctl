rule pixee:java/sanitize-apache-multipart-filename

match
  InstanceMethodCall $c {
    type in (org.apache.commons.fileupload.FileItem, org.apache.commons.fileupload.FileItem)
    name = getName
    args = []
  }

where
  !hasUpstream(
    $c,
    StaticMethodCall {
      type = io.pixee.security.SafeIO
      name = toSimpleFileName
      args = [$c]
    }
  )

require dependency io.pixee:io.pixee.security:1.0
require import org.pixee.security.SafeIO

insert into data flow after $c
  StaticMethodCall {
    type = io.pixee.security.SafeIO
    name = toSimpleFileName
    args = [$c]
  }