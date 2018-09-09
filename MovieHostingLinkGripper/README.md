# Movie Hosting Link Gripper

CMD line tool to simulate browser to get downlaod links for videos/series on different services like Vidoza. 
Links are fetched from aggregating portals (now supports http://alltube.tv only).
 
When all links to movie hosting provider are loaded this tool simulate user clicks by opening 
background independent browser tab, wait for hosting to start loading video and gather direct .mp4 link, 
than save it in json file.

Allows also to send all fetched links to IDM (Internet Download Manager) as main queue items.