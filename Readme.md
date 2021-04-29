### Why this project?
While ThreadPools support dynamic sizes of their thread pools, 
ScheduledExecutors don't do it. I don't why but I guess optimizing a thread pool 
for scheduled tasks is impossible because you will always find a schedule of tasks which
suffers from a poor performance by a bad default configuration.  
E.g. you schedule 10 tasks which take 5s execution time every 30s simultaneously. If the default configuration of
a thread pool with at least 5 threads and at most 10 threads has got a keepAliveTime of 20s, 
then the thread pool shrinks to early from 10 threads to 5 threads because the keepAliveTime is much smaller 
than the sum of the execution time and the pause between executions. And when all tasks are started, 
the thread pool grows up to 10 threads again.
To sum it up, the thread pool will create and destroy threads all the time which makes no sense.

### What is this project?
It is an implementation of a ScheduledExecutorService with a dynamic pool size.
It combines a ScheduledExecutorService with a ExecutorService.
The ScheduledExecutorService with a fixed pool size only schedules the tasks
by passing them to the ExecutorService when they are ready to be executed.
The ExecutorService can be a Threadpool with a dynamic pool size.

### What is the downside of this approach?
As I mentioned above, there is no optimal default configuration. 
It always depends on the schedule of your tasks!
Therefore, you need to observe what kind of tasks are scheduled when and how long they take.
I can't do that for you.

### I found a bug in your code. What shall I do?
If you really find a bug, then you have two options:
* You create a ticket for me in which you describe the bug.
Maybe you can write a JUnit 5 test to reproduce it.
* You do a fork, change the code and do a pull request.
Please explain why this change is worth to be merged.
  I will have a look on it and decide. Please accept my decision even if I reject your pull request.
  It's never personal. I am just a benevolent dictator in this project.

### I found a way to optimize the performance of your code. What shall I do?
I must confess that I am very sceptical about optimizations.
I don't want to start optimizing my code for some applications with special schedules of tasks!
Whenever you optimize the code for one group, it will have negative performance impacts for other groups.
Therefore, be prepared that I will refuse those "optimizations"!   
I don't neither look for a perfect default configuration for my solution because I don't think it exists!

### I like your solution. I want to use it in my project. May I?
If your project is open source AND non-commercial, then the answer is "yes, of course". 
I appreciate an email in which you tell me shortly about your project and how you use my solution.
If your project is closed source and/or commercial, I will not allow you to use this code for two reasons:
1. I am not sure whether this code is good and safe enough to be used in a productive system.
   I cannot and will not guarantee that my solution always works the way programmers expect it.
2. I don't want to run in any liability issues or a law case!
   And I don't want to be sued for code which I wrote in my leisure time for fun.
   
Therefore, use it on your own risk!

### I have got ideas for more solutions. Can I contribute them to this project?
No, thanks. Please create your own project, add your solutions and maintain them. 
This project doesn't need to grow. Sorry.

