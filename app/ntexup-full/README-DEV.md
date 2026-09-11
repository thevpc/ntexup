


############################################
## NETBEANS EXEC
############################################


########### DEBUG 

exec.vmArgs=-agentlib:jdwp=transport=dt_socket,server=n,address=${jpda.address} "-Dnuts.args=-Zy --verbose"
exec.args=-e java ${exec.vmArgs} -classpath %classpath ${exec.mainClass} ${exec.appArgs}
exec.appArgs=show
exec.mainClass=net.thevpc.ntexup.NTexupMain
exec.executable=konsole
jpda.listen=true
exec.workingdir=/home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup/test/ntexup-examples/src/ntexup/examples/mw/antenna-patch-notches/
exec.workingdir=/home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup/test/ntexup-examples/src/ntexup/examples/mw/antenna-patch
