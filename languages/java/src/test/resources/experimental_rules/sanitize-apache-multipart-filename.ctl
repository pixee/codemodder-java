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
      type = io.openpixee.security.SafeIO
      name = toSimpleFileName
      args = [$c]
    }
  )

require dependency io.openpixee:java-security-toolkit:1.0.0
require import org.pixee.security.Filenames

insert into data flow after $c
  StaticMethodCall {
    type = io.openpixee.security.Filenames
    name = toSimpleFileName
    args = [$c]
  }