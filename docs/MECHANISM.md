# Pixel - version control with Minecraft
이 문서는 마인크래프트 pixel 플러그인의 기본적인 작동 방식에 대해 설명합니다.  
본 README.md 문서는 [여기](/README.md)입니다.

## 이 문서의 내용
[전체적인 흐름](#전체적인-흐름)  
[클라이언트 디렉터리와 버전관리 디렉터리](#클라이언트-디렉터리와-버전관리-디렉터리)  
[기존의 월드와 버전관리 월드](#기존의-월드와-버전관리-월드)  
[커맨드라인에서의 `world` 인수](#커맨드라인에서의-world-인수)

## 전체적인 흐름
pixel 플러그인은 월드를 Git이라는 버전관리 도구로 월드 데이터를 관리합니다.  
커맨드라인의 명령 이름은 모두 Git의 그것과 일치하며, 거의 모든 동작을 Git과 최대한 비슷하도록 했습니다.  
물론 아직 Git의 모든 기능 중 pixel 플러그인을 통해 사용할 수 있는 기능은 극히 일부이긴 하지만요.  
  
`/pixel commit`으로 백업지점을 만들면, pixel 플러그인 내부에서는 다음과 같은 일이 일어납니다.
1. 월드를 언로드합니다.
2. 월드 데이터를 버전관리 디렉터리로 복사합니다.
3. 버전관리 디렉터리에서 Git 명령을 통해 commit을 수행합니다.
4. 월드를 다시 로드합니다.
  
`/pixel discard`로 몇 백업지점 뒤로 시간을 되돌리면, 아래와 같은 일이 일어납니다.
1. 버전관리 디렉터리에서 Git 명령을 통해 마지막 commit 이후의 변경사항을 삭제합니다.
2. 월드를 언로드합니다.
3. 버전관리 디렉터리의 내용을 클라이언트 디렉터리로 덮어씁니다.
4. 월드를 다시 로드합니다.
  
이렇듯, 병합을 제외한 모든 기능이 Git에 기반하고 있으며 pixel 플러그인은 월드를 Git을 통해 서버를 껐다 켜거나 하지 않아도 실시간으로 버전관리할 수 있도록 
도와주는 역할을 합니다.  
  
그것을 위해 pixel 플러그인이 수행한 일은 아래에 서술되어있습니다.  
병합에 관해서는 [별도 문서](/docs/MERGING_WORLDS.md)로 분리되어있으니 궁금하시다면 확인해 보세요!

## 클라이언트 디렉터리와 버전관리 디렉터리
클라이언트 디렉터리는 실제 마인크래프트 클라이언트에 서빙되는 맵이 저장되는 디렉터리를,   
버전관리 디렉터리는 플러그인 데이터 디렉터리에 존재하는, Git을 통해 버전관리가 진행되는 디렉터리를 말합니다.  
  
pixel 플러그인은 커맨드를 통해 클라이언트 디렉터리에서 버전관리 디렉터리로 월드 파일을 복사하여 
백업 지점을 만들거나, 혹은 버전관리 디렉터리에서 Git 명령을 수행하고 해당 사항을 클라이언트 디렉터리에 덮어써서
실제 월드에 반영합니다.  
  
## 기존의 월드와 버전관리 월드
pixel 플러그인을 처음 초기화하면, 일반적이라면 기존에 존재하던 세 월드에 두 월드가 추가됩니다.  
편의상 `server.properties`의 `level-name` 값이 `world`라는 가정 하에 설명하면, 기존에는 
`world`, `world_nether`, `world_the_end` 세 월드였던 상태에서 `__void__`, `world_overworld` 가 추가됩니다.  
  
이때 버전관리가 진행되는 월드는 `world_overworld`, `world_nether`, `world_the_end` 뿐이므로, `world` 월드에서 변경사항을 만들지 않도록 주의해야합니다.
    
그리고 pixel 플러그인은 활성화된 시점에서, 그 이후로 서버에 접속하거나 네더 포탈로 이동하는 등의
모든 `world`월드로 진입하려는 플레이어를 `world_overworld`로 대신 이동시킵니다.  
이것은 `world` 월드가 버전관리가 수행되지 않기 때문에 플레이어가 변경사항을 만들 월드를 헷갈리게 하지 않게 하기 위해서이며,
만약 어떠한 이유로 `world` 월드에 갈 일이 생기면 `/pixel tp`커맨드를 사용할 수 있습니다.  
  
마지막으로, `__void__` 월드는 커맨드 수행 중 플레이어에 의해 또다른 변경사항이 생기는 것을 막고, 월드를 언로드하여 데이터를 읽기 위해
플레이어가 커맨드 수행 중 잠시 머무르게 되는 월드입니다.  
이 월드에 진입한 순간부터 다시 복귀될때까지는 중력이 사라지기 때문에 떨어져 죽지 않으며, 만약 커맨드 실패로 이 월드에 남겨졌다고 하더라도 
`/pixel tp` 명령을 통해 기존 월드로 돌아갈 수 있습니다.  
  
## 커맨드라인에서의 `world` 인수
거의 모든 버전관리 명령에서 `world` 인수를 받고있는데, 이 인수는 
클라이언트 디렉터리의 변경사항을 버전관리 디렉터리로의, 혹은 그 반대의 변경 사항을 반영할 월드를 한정합니다.  
  
다시말해, 클라이언트 디렉터리에서 어떤 변경사항이 생겨도 `/pixel commit` 커맨드의 `world` 인수에 해당하지 않는 월드는 버전관리 디렉터리에 복사되지 않습니다.  
역으로, 버전관리 디렉터리에서 Git에 의해 어떤 변경사항이 생겨도 `/pixel checkout` 등의 명령에서 `world` 인수에 해당하지 않는 월드는 클라이언트 디렉터리에 덮어씌워지지 않습니다.  
  
실제로 세 월드는 서로 다른 로컬저장소를 통해 관리되기 때문에 이 인수는 반드시 필요합니다.
  