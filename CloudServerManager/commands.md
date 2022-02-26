## Sending input to stdin

### Windows

Powershell
```sh
Get-Content c.txt -Wait | java -jar .\app.jar
```
Where `c.txt` is pipeline like file


### Linux

Bash
```sh
tail -f console.in | java -jar app.jar

# nohup
nohup tail -f console.in | nohup java -jar app.jar &
```
Where `console.in` is pipeline like file

---

## Running in background

### Windows

Powershell
```sh
Start-Process -NoNewWindow java -jar .\app.jar

Start-Process -NoNewWindow -filepath 'powershell.exe' -argumentlist '-c ping google.com' -redirectstandardoutput "%temp%\out-null"

Start-Process -NoNewWindow -filepath 'powershell.exe' -argumentlist '-c ping google.com > $null '

(Start-Process -NoNewWindow -filepath 'powershell.exe' -passthru -argumentlist '-c ping google.com > $null ').Id
```


### Linux

Bash
```sh
nohup java -jar ./app.jar > /dev/null 2>&1 & echo $! > run.pid
```


