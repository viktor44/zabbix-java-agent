setlocal

java -javaagent:target/zabbix-java-agent-0.1.0-SNAPSHOT.jar=etc/zabbix_agentd.conf -cp target/test-classes com.github.zabbix.agent.TestAppForAgent

endlocal
