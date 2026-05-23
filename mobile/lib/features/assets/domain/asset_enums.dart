import 'package:freezed_annotation/freezed_annotation.dart';

enum AssetType {
  @JsonValue('IMAGE')
  image,
  @JsonValue('VIDEO')
  video,
  @JsonValue('DOCUMENT')
  document,
  @JsonValue('OTHER')
  other,
}

enum AssetCategory {
  @JsonValue('RAW')
  raw,
  @JsonValue('LOGO')
  logo,
  @JsonValue('CREATIVE')
  creative,
}

enum AssetStatus {
  @JsonValue('ACTIVE')
  active,
  @JsonValue('ARCHIVED')
  archived,
  @JsonValue('DELETED')
  deleted,
}
