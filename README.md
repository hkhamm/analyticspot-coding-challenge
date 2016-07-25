done - There should be a constructor which takes a URL.

done - The object returned by your constructor should have a public method called "send". "send" takes one argument: a string.

- When send is called it should send the passed string to the URL with which it was constructed as the body of an HTTP POST.

- The method should return to the caller immediately, without blocking waiting for a reply. However, internally it should ensure it gets called back when a response is received from the server.

- If the server responds with success (any 20x response code) you're done. If not, the retry logic described below should be used.

- Failed send attempts should be retried with an exponential backoff. The first retry should be attempted after 1 second, then 2 seconds, then 4 seconds, etc. If it fails 10 times the item should be dropped.

- You must ensure that there are no more than 10 events pending retry at any one time so as not to use up too much memory holding retry state.

- "send" calls always result in at least one attempt to send the data.

- If a "send" attempt fails then it is queued for retry unless the retry queue is full at the time of the failure.

- There should be a limited number of threads regardless of how quickly send is called, how many calls require retry, and how much latency there is between your client and the server.

- You are free to use Java's built-in HTTP libraries or use other libraries of your choosing. We use AsyncHttpClient but feel free to use something else if you prefer.