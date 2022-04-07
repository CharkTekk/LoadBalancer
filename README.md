# LoadBalancer
Simple loadbalancer application using *Netty*

How to use:

```java
    // The Number is for
    new LoadBalancer(new InetSocketAddress(25565), Proxys);

````

The proxy number is used to set the number for BungeeCord, please remember that Java starts at 0.
Your BungeeCords should start at port 20000 and go up in 1 steps

Port example:
20000 ,20001, 20002

A total of 10 BungeeCord can be processed !!

The LoadBalancer was rebuilt the original src is with: https://github.com/OriginalRooks
