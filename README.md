# LoadBalancer
Simple loadbalancer application using *Netty*

How to use:

```java
    
    Logic for you Project
    new LoadBalancer(new InetSocketAddress(25565), amount);

    or use the jar:
    java -jar LoadBalancer.jar localhost port amount

````

The proxy number is used to set the number for BungeeCord, please remember that Java starts at 0.
Your BungeeCords should start at port 20000 and go up in 1 steps

Port example:
20000 ,20001, 20002

The LoadBalancer was rebuilt the original src is with: https://github.com/OriginalRooks
