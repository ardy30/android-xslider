# android\-xslider

自定义的 Slider，可以实现下图的效果：

![效果图][1]

## Gradle

[![](https://www.jitpack.io/v/wuzhendev/android-xslider.svg)](https://www.jitpack.io/#wuzhendev/android-xslider)

``` groovy
repositories {
    maven {
        url "https://www.jitpack.io"
    }
}

dependencies {
    compile 'com.github.wuzhendev:android-xslider:x.y.z'
}
```

## Attrs

``` xml
<!--  -->
<attr name="android:enabled" />

<!-- 滑动条的颜色 -->
<attr name="xslider_trackColor" format="reference|color" />

<!-- 进度条的颜色 -->
<attr name="xslider_progressColor" format="reference|color" />

<!-- 滑块的颜色 -->
<attr name="xslider_thumbColor" format="reference|color" />

<!-- 滑动条的高度 -->
<attr name="xslider_trackSize" format="reference|dimension" />

<!-- 滑动条的值 -->
<attr name="xslider_value" format="reference|integer" />

<!-- 滑动条的最小值 -->
<attr name="xslider_minValue" format="reference|integer" />

<!-- 滑动条的最大值 -->
<attr name="xslider_maxValue" format="reference|integer" />

<!-- 滑块的形状: 圆形、圆角矩形 -->
<attr name="xslider_thumbType" format="integer">
    <enum name="oval" value="0" />
    <enum name="rectangle" value="1" />
</attr>

<!-- 滑块的半径, 如果是圆形该值是圆形的半径, 如果是圆角矩形是圆角的半径 -->
<attr name="xslider_thumbRadius" format="reference|dimension" />

<!-- 滑块的宽度 -->
<attr name="xslider_thumbWidth" format="reference|dimension" />

<!-- 滑块的高度 -->
<attr name="xslider_thumbHeight" format="reference|dimension" />
```

## Sample

[Sample sources][2]

[Sample APK](https://github.com/wuzhendev/android-xslider/raw/master/assets/XSlider_Demo_v1_0_0.apk)

## License

```
Copyright 2016 wuzhen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[1]: ./assets/1.jpg
[2]: ./samples
