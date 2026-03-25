# 프로젝트 목적
본 프로젝트는 Compose를 이용해서 Android에서 사용할 수 있는 이미지 피커 라이브러리를 구현한다. 
v1 에서 구현할 기능으로 이미지 선택의 핵심 흐름에 집중한다.

### 개발 목표
- 제 앱에서 재사용 가능한 Image Picker 컴포넌트 구현
- Compose 기반 UI 및 상태 관리 구조 설계
- 이미지 선택과 편집의 최소 기능을 안정적으로 제공
- 비디오 지원 등 확장 가능한 구조 확보

## v1 기능 범위
### 포함 기능

1. 이미지 접근 권한 요청
2. 기기 갤러리 이미지 목록 조회
3. 이미지 최대 10장 선택 제한
4. 선택 이미지 편집
    - 90도 단위 회전
    - 크롭
5. 드래그 멀티 선택    
6. 선택 결과 반환

## 3. 핵심 요구사항

## 3.1 권한 처리

### 기능 설명
사용자가 갤러리 이미지를 선택할 수 있도록 기기 저장소 또는 사진 접근 권한을 요청한다.

### 요구사항

- 최초 진입 시 필요한 권한이 없으면 권한 요청을 수행한다.
- 권한이 허용된 경우 이미지 목록을 조회한다.
- 권한이 영구 거부된 경우 설정 화면 이동 유도를 제공한다.

### 성공 조건
- 사용자가 권한을 허용하면 이미지 목록 화면으로 진입할 수 있어야 한다.
---

## 3.2 이미지 목록 조회

### 기능 설명
기기 갤러리에 저장된 이미지를 조회하여 Grid 형태로 표시한다.

### 요구사항
- 갤러리 앨범 명으로 접근할 수 있도록 한다.
- 최신 이미지 순으로 정렬하여 표시한다.
- 각 이미지는 썸네일 형태로 표시한다.
- 스크롤 가능한 Grid UI를 제공한다.
- 이미지 로딩 중에도 UI가 비정상적으로 멈추지 않아야 한다.

### 성공 조건
- 사용자는 기기 갤러리에 있는 이미지들을 목록 형태로 확인할 수 있어야 한다.

---
## 3.3 이미지 선택
### 기능 설명
사용자는 갤러리 목록에서 이미지를 선택할 수 있다.

### 요구사항
- 이미지는 탭을 통해 선택/해제할 수 있다.
- 최대 10장까지 선택할 수 있다.
- 10장 선택 이후 추가 선택 시 제한 안내를 제공한다.
- 선택된 이미지는 UI상에서 명확하게 구분되어야 한다.
- 선택된 순서는 grid view의 item에 숫자로 표시된다.
- 드래그로 멀티 선택이 가능하다.
- 선택 개수를 화면에서 확인할 수 있어야 한다.

### 성공 조건
- 사용자는 최대 10장까지 이미지를 선택할 수 있어야 한다.
- 선택 수 초과 시 시스템이 추가 선택을 막아야 한다.

### 정책

- 최대 선택 개수: 10장
- 11번째 이미지 선택 시 선택되지 않아야 함
- 제한 메시지 예시:
    
    `이미지는 최대 10장까지 선택할 수 있습니다.`
    

---

## 3.4 이미지 편집

### 기능 설명
사용자는 선택한 이미지에 대해 기본 편집 기능을 수행할 수 있다.

### 지원 기능
- 회전
- 크롭

---

### 3.4.1 회전

### 요구사항
- 사용자는 이미지를 90도 단위로 회전할 수 있다.
- 회전은 버튼 탭으로 수행한다.
- 회전 결과는 즉시 미리보기 화면에 반영되어야 한다.

### 성공 조건
- 사용자가 회전 버튼을 누를 때마다 이미지가 90도씩 회전해야 한다.

### 제약사항
- v1에서는 자유 각도 회전을 지원하지 않는다.
- 회전 방향은 시계 방향 90도로 제한한다.

---

### 3.4.2 크롭

### 요구사항

- 사용자는 이미지의 크롭 영역을 지정할 수 있어야 한다.
- 크롭 영역은 화면에서 시각적으로 확인 가능해야 한다.
- 크롭 완료 시 지정한 영역만 잘린 결과를 생성해야 한다.

### 성공 조건
- 사용자가 지정한 영역 기준으로 이미지가 잘려 결과에 반영되어야 한다.

### 제약사항
- v1에서는 크롭 비율 옵션을 제한적으로 제공한다.
- 고급 편집(확대/축소 기반 정교한 편집, 다중 비율 프리셋 다수 제공 등)은 제외한다.

---

## 3.5 결과 반환

### 기능 설명

사용자는 이미지 선택 및 편집 완료 후 결과를 호출한 화면으로 반환할 수 있어야 한다.

### 요구사항

- 선택 완료 시 선택된 이미지 목록을 반환한다.
- 편집이 적용된 이미지는 편집 결과 기준으로 반환한다.
- 원본 URI와 편집 결과 URI를 구분할 수 있어야 한다.

### 성공 조건

- 호출 측에서 선택/편집 결과를 받아 후속 처리를 수행할 수 있어야 한다.

### 반환 데이터 예시

```
dataclassPickerResult(
valitems:List<PickedImage>
)

dataclassPickedImage(
valoriginalUri:Uri,
valeditedUri:Uri?=null,
valrotationDegrees:Int=0,
valisCropped:Boolean=false
)
```

---

## 4. 사용자 흐름

## 4.1 기본 선택 흐름

1. 사용자가 Image Picker를 실행한다.
2. 권한이 없으면 권한 요청 UI를 표시한다.
3. 권한 허용 후 갤러리 이미지 목록을 조회한다.
4. 사용자가 이미지를 탭 혹은 드래그하여 선택한다.
5. 최대 10장까지 선택한다.
6. 완료 버튼을 눌러 선택 결과를 반환한다.

---

## 4.2 편집 흐름

1. 사용자가 선택한 이미지의 체크박스를 제외한 영역을 누르면 편집 화면에서 열린다.
2. 회전 또는 크롭을 수행한다.
3. 편집 결과를 저장한다.
4. 편집 완료 후 선택 결과에 반영한다.
5. 최종 완료 시 호출 측으로 반환한다.

---

## 5. 화면 단위 요구사항

## 5.1 권한 요청 화면

### 표시 요소
- 설정 이동 버튼(필요 시)

### 상태
- 최초 요청 전
- 거부됨
- 영구 거부됨

---

## 5.2 이미지 목록 화면

### 표시 요소
- 상단 바
- 선택 개수 표시
- 완료 버튼
- Grid 형태 이미지 썸네일 목록

### 상태
- 로딩
- 데이터 있음
- 데이터 없음
- 권한 없음

---

## 5.3 편집 화면

### 표시 요소
- 원본/편집 이미지 미리보기
- 회전 버튼
- 크롭 버튼
- 취소 버튼
- 완료 버튼

### 상태
- 편집 전
- 회전 적용
- 크롭 적용
- 저장 중

---

## 6. 예외 처리

### 6.1 권한 거부
- 권한 거부 시 이미지 목록을 노출하지 않는다.
- 권한 필요 안내 문구를 표시한다.

### 6.2 이미지 없음
- 갤러리에 이미지가 없는 경우 빈 상태 UI를 표시한다.

### 6.3 선택 개수 초과
- 10장 초과 선택 시 추가 선택을 막고 안내 메시지를 표시한다.

### 6.4 편집 실패
- 크롭 또는 회전 처리 실패 시 오류 메시지를 표시한다.
- 원본 이미지는 손상되지 않아야 한다.

### 6.5 결과 저장 실패
- 편집 결과 파일 저장 실패 시 재시도 또는 취소 선택지를 제공한다.

---

# 프로젝트 구조 및 작업 가이드

## 모듈 구성

프로젝트는 두 모듈로 구성된다.

- **`:app`** — 라이브러리 사용 방식을 보여주는 샘플 앱 (`com.universe.dynamicimagepicker`)
- **`:imagepicker`** — 실제 라이브러리 구현체 (`com.universe.imagepicker`)

> 기능 구현은 반드시 `:imagepicker` 모듈에서 수행한다. `:app`은 통합 방식만 보여주며 건드리지 않는 것을 원칙으로 한다.

---

## 아키텍처 패턴: MVI

전체 구조는 **MVI (Model-View-Intent)** 패턴을 따른다.

```
사용자 액션 → Intent → ViewModel → State 갱신 → Composable 렌더링
                                  ↓
                             Effect (Channel) → 일회성 사이드 이펙트
```

- **State**: UI에 표시할 불변 데이터. ViewModel의 `StateFlow`로 관리한다.
- **Intent**: 사용자 또는 시스템이 발생시키는 이벤트. sealed class로 정의한다.
- **Effect**: 화면 이동, 토스트, 권한 요청 등 한 번만 소비되는 이벤트. `Channel`로 관리한다.

---

## 레이어 구성

```
presentation/
  picker/          ← 갤러리 선택 화면 (Screen, State, Intent, Effect, ViewModel, Factory)
  editor/          ← 이미지 편집 화면 (Screen, State, Intent, Effect, ViewModel, Factory)
  gallery/         ← Gallery Grid 컴포넌트 (GalleryScreen, GalleryGridItem, AlbumDropdown)
  component/       ← 재사용 UI 컴포넌트 (TopBarWithCount, SelectionBadge)
  utils/           ← 드래그 멀티 선택 헬퍼 (photoGridDragHandler modifier)
domain/
  model/           ← 도메인 모델 (GalleryImage, GalleryAlbum, PickedImage, PickerResult, CropRect, PermissionStatus)
  repository/      ← Repository 인터페이스 (GalleryRepository, ImageEditRepository)
  usecase/         ← UseCase (GetGalleryAlbums, GetImagesInAlbum, RotateImage, CropImage, CheckPermission)
data/
  source/          ← 데이터 소스 (MediaStoreDataSource, ImageFileDataSource)
  repository/      ← Repository 구현체 (GalleryRepositoryImpl, ImageEditRepositoryImpl)
```

---

## 주요 파일 역할

| 파일 | 역할 |
|------|------|
| `DynamicImagePicker.kt` | 라이브러리 퍼블릭 진입점. `DynamicImagePicker.Content(config, onResult, onCancel)` 제공 |
| `ImagePickerConfig.kt` | 라이브러리 설정 (`maxSelectionCount`, `showAlbumSelector`, `allowEditing`) |
| `ImagePickerScreen.kt` | 권한 처리 + 화면 라우팅 (갤러리/권한 거부 화면 분기) |
| `GalleryScreen.kt` | 3열 Grid, 드래그 멀티 선택, 상단 바, 선택 제한 스낵바 |
| `GalleryGridItem.kt` | 썸네일 + 선택 오버레이 + SelectionBadge |
| `ImagePickerViewModel.kt` | 갤러리 선택 전체 상태 관리, 편집 결과 병합, 최종 결과 반환 |
| `EditorScreen.kt` | 이미지 편집 UI (회전/크롭) |
| `EditorViewModel.kt` | 편집 상태 관리. 항상 originalUri 기준으로 회전하여 JPEG 화질 손실 방지 |
| `MediaStoreDataSource.kt` | MediaStore ContentResolver로 이미지/앨범 조회 (API 26-36 호환) |
| `ImageFileDataSource.kt` | 비트맵 회전·크롭 처리. 캐시: `context.cacheDir/imagepicker_edits/` |
| `Utils.kt` | `photoGridDragHandler` Modifier — 롱프레스 후 드래그 멀티 선택 구현 |

---

## 도메인 모델 요약

```kotlin
// 최종 반환 결과
data class PickerResult(val items: List<PickedImage>)

data class PickedImage(
    val originalUri: Uri,
    val editedUri: Uri? = null,       // 편집 없으면 null
    val rotationDegrees: Int = 0,
    val cropRect: CropRect? = null    // isCropped는 cropRect != null 로 판단
)

// 정규화 크롭 좌표 [0f, 1f]
data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    companion object { val FULL = CropRect(0f, 0f, 1f, 1f) }
}

data class GalleryImage(val id: Long, val uri: Uri, val albumId: String, ...)
data class GalleryAlbum(val id: String, val name: String, val coverUri: Uri, val imageCount: Int)

enum class PermissionStatus { GRANTED, PARTIALLY_GRANTED, DENIED, PERMANENTLY_DENIED }
```

---

## 작업 시 접근 방법

### 1. 새 기능 추가
1. `domain/model/`에 필요한 데이터 모델 추가 또는 기존 모델 수정
2. 필요한 경우 `domain/repository/` 인터페이스에 메서드 추가
3. `domain/usecase/`에 UseCase 추가
4. `data/source/` 및 `data/repository/`에 구현체 반영
5. 해당 화면의 `*Intent`, `*State`, `*Effect` sealed class에 항목 추가
6. ViewModel의 `handleIntent()`에 처리 로직 추가
7. Composable에서 State/Effect 소비

### 2. UI 수정
- 재사용 컴포넌트는 `presentation/component/`에 위치
- 화면별 컴포넌트는 해당 화면 패키지 내에서 관리 (`picker/`, `gallery/`, `editor/`)
- Compose 상태는 ViewModel의 `StateFlow`를 `collectAsStateWithLifecycle()`로 수집

### 3. 드래그 선택 수정
- `presentation/utils/Utils.kt`의 `photoGridDragHandler` Modifier 수정
- 롱프레스 감지 → 드래그 시작 → 아이템 하이트박스 기반 선택 순서로 동작

### 4. 편집 기능 수정
- `EditorViewModel`은 항상 `originalUri`를 기준으로 회전을 적용한다 (JPEG 재압축 누적 방지)
- 크롭은 현재 `previewUri`를 기준으로 적용
- 편집 완료 시 `PickedImage(editedUri = ..., rotationDegrees = ..., cropRect = ...)`로 반환

### 5. 권한 처리 수정
- 권한 분기 로직: `ImagePickerScreen.kt`
- 상태 표현: `PermissionStatus` enum
- Android 14+ 부분 권한(`READ_MEDIA_VISUAL_USER_SELECTED`)은 `PARTIALLY_GRANTED`로 처리

---

## 빌드 설정 요약

- **compileSdk / targetSdk**: 36
- **minSdk**: 26
- **Kotlin**: 2.0.21
- **Compose BOM**: 2025.05.00
- **이미지 로딩**: Coil 2.7.0 (`AsyncImage`)
- **비동기**: Kotlin Coroutines 1.9.0
- **ViewModel**: AndroidX Lifecycle 2.9.0

---

## 라이브러리 통합 방법 (`:app` 참고)

```kotlin
// build.gradle.kts
implementation(project(":imagepicker"))

// 사용
DynamicImagePicker.Content(
    config = ImagePickerConfig(maxSelectionCount = 10),
    onResult = { result: PickerResult -> /* items: List<PickedImage> */ },
    onCancel = { /* 취소 처리 */ }
)
```