# IDE Dart Plugin

## 简介
IDE Dart Tools 是一个用于 Dart 开发的 IntelliJ IDEA 插件，旨在提供便捷的代码生成和管理功能，包括但不限于：
- 生成命名参数构造函数
- 生成 `copyWith` 方法
- 生成 `toMap()` 和 `fromMap()` 方法
- 生成 `toJson()` 和 `fromJson()` 方法

## 安装
1. 打开 IntelliJ IDEA。
2. 进入 `File` -> `Settings` (或 `Preferences` 在 macOS 上)。
3. 选择 `Plugins`。
4. 点击 `Marketplace` 标签，搜索 `IDE Dart Tools`。
5. 点击 `Install` 按钮，然后重启 IntelliJ IDEA。

## 使用说明
### 生成命名参数构造函数
1. 打开一个 Dart 类文件。
2. 右键点击编辑器中的类名。
3. 选择 `Generate` -> `Named Argument Constructor`。

### 生成 `copyWith` 方法
1. 打开一个 Dart 类文件。
2. 右键点击编辑器中的类名。
3. 选择 `Generate` -> `Copy`。

### 生成 `toMap()` 和 `fromMap()` 方法
1. 打开一个 Dart 类文件。
2. 右键点击编辑器中的类名。
3. 选择 `Generate` -> `toMap() and fromMap()`。

### 生成 `toJson()` 和 `fromJson()` 方法
1. 打开一个 Dart 类文件。
2. 右键点击编辑器中的类名。
3. 选择 `Generate` -> `toJson() and fromJson()`。

## 运行|编译|发布

### 运行

Tasks => intellij platform => runIde

### 编译

Tasks => intellij platform => buildPlugin

### 发布

Tasks => intellij platform => publishPlugin