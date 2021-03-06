# Pixel - version control with Minecraft
이 문서는 마인크래프트 pixel 플러그인이 버전관리를 위해 새로운 월드를 또 만든 이유에 대해 설명합니다.  
본 README.md 문서는 [여기](/README.md)입니다.

## 이 문서의 내용
[월드의 로드와 언로드](#월드의-로드와-언로드)  
[그럼 왜 새로운 월드를 또 만들었나?](#그럼-왜-새로운-월드를-또-만들었나)  
&nbsp;&nbsp;&nbsp;&nbsp;[월드 언로드의 전제조건](#월드-언로드의-전제조건)  
&nbsp;&nbsp;&nbsp;&nbsp;[world_overworld 월드](#world_overworld-월드)  
&nbsp;&nbsp;&nbsp;&nbsp;[\_\_void__ 월드](#__void__-월드)


## 월드의 로드와 언로드
클라이언트 디렉터리의 마인크래프트 월드 데이터는 실시간으로 변경됩니다.  
그렇기 때문에, 월드가 로드된 상태로 서버가 돌아가는 중에 그 데이터를 읽으려고 시도하면,
일관성이 깨진 파일을 읽게되어 이후에 해당 파일로 다시 덮어씌웠을 때 월드가 망가지는 일이 발생합니다.  
역으로 파일을 덮어씌울 때도 일관성이 무너지기 때문에, 월드 데이터를 읽거나 쓸 때는 반드시 서버가 이 파일들로부터 
손을 떼게 할 필요가 있습니다.  
  
그러기 위해 월드를 언로드하고, 로드하는 과정이 생겨났습니다.  
언로드는 조건을 만족하면 빠르게 수행되지만, 로드는 서버를 켤 때 맵을 로드하는 그 속도 그대로 로드하기 때문에,
병합을 제외한 모든 커맨드작업 시 걸리는 시간은 95% 이상이 모두 맵을 로드하는데 소요됩니다.

## 그럼 왜 새로운 월드를 또 만들었나?
### 월드 언로드의 전제조건
월드의 언로드가 정상적으로 수행되기 위한 전제 조건에는 두 가지가 있습니다.
- 월드에 현재 어떤 플레이어도 없을 것
- 월드에 실시간으로 반드시 계속 읽거나 써야하는 데이터가 포함되어있지 않을 것

이 때 실시간으로 반드시 계속 읽거나 써야하는 데이터란 플레이어 데이터, 도전과제 데이터, 통계 데이터를 포함하고 있는 월드를 말합니다.  
spigot 서버의 월드 시스템은 조금 특이한데, 서버를 처음 만들었을 때 생성되는 세 월드 중 `world` 월드에만
위의 세 데이터를 저장하고, 나머지 두 `nether`와 `the_end` 월드에는 저장하지 않습니다.  
  
여기서 문제는 그 세 데이터가 각 월드와 관계없는 독립적인 데이터임에도 월드에 종속되어, 실시간으로 반드시 읽어야 하는 데이터가 포함되어있기 때문에
`world` 월드는 서버가 켜져있는 한 언로드가 불가능하다는 점입니다.  

### `world_overworld` 월드
`world_overworld` 월드는 두 번째 전제조건을 만족시키기 위해 만들었습니다.  
어쩄든 저 데이터만 포함되어있지 않으면 되고, 그 세 데이터는 월드와 독립적인 데이터기 때문에 저 세 데이터를 제외한 나머지 데이터만 존재하는 월드만 있으면 됐습니다.  
  
그리고 spigot 은 이런 월드 시스템 때문인지는 몰라도 기본으로 주어지는 세 월드를 제외한 다른 월드를 만들 수도 있고 거기로 출입도 자유롭게 할 수 있었습니다.  
그리하여 생성시점의 `world` 월드의 복제이되, 그 데이터를 제외한 월드를 생성하게 되었고 그 월드의 이름이 `world_overworld`가 된 것입니다.  
그리고 나머지, 기본적으로 `world` 월드로 이동되는 이벤트를 전부 잡아 `world_overworld`로 보내주는 것이죠.  

### `__void__` 월드
`__void__` 월드는 첫 번째 전제조건을 만족시키기 위해 만들었습니다.  
월드를 언로드하려면 그 월드에 아무도 없어야하는데, 플레이어를 이동시킬 수야 있지만 어디로 이동시키느냐가 문제였죠.  
기존의 `world` 월드로 이동시킬수도 있었지만, 그럴 경우 플러그인 사용 초기에 '뭐야 변경사항 다 날아갔잖아'라는 오해를 살 수 있었기에 그것은 피하고싶었습니다.  
  
그래서, 아무것도 할 수 없고 그냥 텅 빈 하늘만 볼 수 있는 공허세계인 `__void__` 월드를 만들었습니다.  
이 월드로 이동되면 플레이어에게 반영되던 중력이 제거되어 공중에 둥둥 뜬 상태가 됩니다. 그래서 떨어져 죽거나 할 일도 없죠.  
  
사실 이 플러그인을 통해 뭔가의 커맨드 명령을 내려서 이 세계로 이동된 시점이면, '다음 단계로 넘어가기 위한 중간과정'을 수행중인 것이라고 생각했습니다.  
그래서 잠시라도 명령을 수행하는동안 공허세계에서 멍이라도 때리는건 어떨까, 하는 생각도 어느정도 있었어요.  
  
뭐, 아무튼 이 월드는 월드를 언로드하기 위해 플레이어를 잠시 해당 월드에서 제거하기 위한 월드입니다.