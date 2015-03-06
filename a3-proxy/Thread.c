#include "Thread.h"
#include <errno.h>
#include <stdlib.h>


// This is provide simply as a sample of how to call the various 
// pthread functions that you will need to use to implement multi-threading
// You can use the functions in here or you can just use the pthread routines
// directly. 





// When using this collection of functions the -pthread option needs to be 
// include on the line doing the linking.
// For example:
//   gcc -pthread  main.c Thread.c
//


// the constructor, initializes the elements within the thread class
void *createThread(void* (*func)(void*), void * parm) {
	struct Thread *thread;
	thread = malloc(sizeof(struct Thread));
	if (thread) {
		thread->entry_pt = func;
		thread->arg = parm;
	}
	return thread;
}


// some essential functions

// create the thread and run it
int runThread(void *vthread, pthread_attr_t *attr) 
{ 
	struct Thread *thread = vthread;
	if (vthread) {
		return pthread_create(&thread->id, attr, thread->entry_pt, thread->arg );
	}
	return -10;
}

// Terminate this thread
int cancelThread(void * vthread)
{
	struct Thread *thread = vthread;
	return pthread_cancel(thread->id);
}

int joinThread(void * vthread, void **thread_return)
{
	struct Thread *thread = vthread;
	return pthread_join(thread->id, thread_return);
}

int detachThread(void *vthread) // reaps a thread
{
	struct Thread *thread = vthread;
	return pthread_detach(thread->id);
}


// some accessors

// get the ThreadID
pthread_t getThreadID(void *vthread)
{
	struct Thread *thread = vthread;
	return thread->id;
}


// get the arguments passed to the process entry_point
void* getThreadArg(void * vthread)
{
	struct Thread *thread = vthread;
	return thread->arg;
}
