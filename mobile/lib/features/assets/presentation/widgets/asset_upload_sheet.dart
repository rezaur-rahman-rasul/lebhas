import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import 'package:file_picker/file_picker.dart';
import 'package:dio/dio.dart';
import 'package:lebhas_creative_maker/shared/extensions/context_extensions.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_button.dart';
import 'package:lebhas_creative_maker/shared/widgets/app_text_field.dart';
import 'package:lebhas_creative_maker/features/assets/domain/asset_enums.dart';
import 'package:lebhas_creative_maker/features/assets/domain/asset_models.dart';
import 'package:lebhas_creative_maker/features/assets/state/asset_controller.dart';

class AssetUploadSheet extends ConsumerStatefulWidget {
  final String projectId;
  const AssetUploadSheet({super.key, required this.projectId});

  @override
  ConsumerState<AssetUploadSheet> createState() => _AssetUploadSheetState();
}

class _AssetUploadSheetState extends ConsumerState<AssetUploadSheet> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _descriptionController = TextEditingController();
  
  File? _selectedFile;
  AssetCategory _selectedCategory = AssetCategory.raw;
  String? _fileName;
  int? _fileSize;

  final ImagePicker _picker = ImagePicker();

  Future<void> _pickImage() async {
    final XFile? image = await _picker.pickImage(source: ImageSource.gallery);
    if (image != null) {
      setState(() {
        _selectedFile = File(image.path);
        _fileName = image.name;
      });
      _nameController.text = image.name.split('.').first;
      _checkFileSize();
    }
  }

  Future<void> _pickFile() async {
    FilePickerResult? result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['jpg', 'jpeg', 'png', 'webp', 'mp4', 'mov', 'svg'],
    );

    if (result != null) {
      final file = File(result.files.single.path!);
      setState(() {
        _selectedFile = file;
        _fileName = result.files.single.name;
        _fileSize = result.files.single.size;
      });
      _nameController.text = _fileName!.split('.').first;
      _checkFileSize();
    }
  }

  void _checkFileSize() async {
    if (_selectedFile == null) return;
    final size = await _selectedFile!.length();
    _fileSize = size;
    
    double maxSizeMB = 10; 
    if (_selectedCategory == AssetCategory.logo) maxSizeMB = 5;
    if (_fileName?.toLowerCase().endsWith('.mp4') == true || _fileName?.toLowerCase().endsWith('.mov') == true) {
      maxSizeMB = 200;
    }

    if (size > maxSizeMB * 1024 * 1024) {
      if (mounted) {
        context.showSnackBar('File size exceeds $maxSizeMB MB limit');
        setState(() {
          _selectedFile = null;
          _fileName = null;
        });
      }
    }
  }

  Future<void> _upload() async {
    if (!_formKey.currentState!.validate() || _selectedFile == null) return;

    final request = AssetUploadRequest(
      assetCategory: _selectedCategory,
      displayName: _nameController.text,
      description: _descriptionController.text.isNotEmpty ? _descriptionController.text : null,
    );

    final multipartFile = await MultipartFile.fromFile(
      _selectedFile!.path,
      filename: _fileName,
    );

    await ref.read(assetControllerProvider.notifier).uploadAsset(
      projectId: widget.projectId,
      request: request,
      file: multipartFile,
    );

    if (mounted && !ref.read(assetControllerProvider).hasError) {
      Navigator.pop(context);
      context.showSnackBar('Asset uploaded successfully');
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final status = ref.watch(assetControllerProvider);

    return Container(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom + 24,
        top: 24,
        left: 24,
        right: 24,
      ),
      decoration: BoxDecoration(
        color: context.colorScheme.surface,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
      ),
      child: SingleChildScrollView(
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            mainAxisSize: MainAxisSize.min,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: context.colorScheme.outlineVariant,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 24),
              Text(
                'Upload Project Asset',
                style: context.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 24),
              
              if (_selectedFile == null)
                Row(
                  children: [
                    Expanded(
                      child: _PickerCard(
                        icon: Icons.image_outlined,
                        label: 'Gallery',
                        onTap: _pickImage,
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: _PickerCard(
                        icon: Icons.file_present_outlined,
                        label: 'Files',
                        onTap: _pickFile,
                      ),
                    ),
                  ],
                )
              else
                _FilePreview(
                  fileName: _fileName ?? '',
                  fileSize: _fileSize ?? 0,
                  onRemove: () => setState(() => _selectedFile = null),
                ),

              const SizedBox(height: 24),
              DropdownButtonFormField<AssetCategory>(
                value: _selectedCategory,
                decoration: const InputDecoration(labelText: 'Asset Category'),
                items: AssetCategory.values.map((cat) => DropdownMenuItem(
                  value: cat,
                  child: Text(cat.name.toUpperCase()),
                )).toList(),
                onChanged: (val) {
                  if (val != null) {
                    setState(() => _selectedCategory = val);
                    _checkFileSize();
                  }
                },
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Display Name',
                controller: _nameController,
                validator: (v) => v == null || v.isEmpty ? 'Name is required' : null,
              ),
              const SizedBox(height: 16),
              AppTextField(
                label: 'Description (Optional)',
                controller: _descriptionController,
                maxLines: 2,
              ),
              const SizedBox(height: 32),
              AppButton(
                text: 'Upload Asset',
                isLoading: status.isLoading,
                onPressed: _upload,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PickerCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _PickerCard({required this.icon, required this.label, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 24),
        decoration: BoxDecoration(
          border: Border.all(color: context.colorScheme.outlineVariant),
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          children: [
            Icon(icon, size: 32, color: context.colorScheme.primary),
            const SizedBox(height: 8),
            Text(label, style: context.textTheme.labelLarge),
          ],
        ),
      ),
    );
  }
}

class _FilePreview extends StatelessWidget {
  final String fileName;
  final int fileSize;
  final VoidCallback onRemove;

  const _FilePreview({required this.fileName, required this.fileSize, required this.onRemove});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: context.colorScheme.primaryContainer.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: context.colorScheme.primary.withValues(alpha: 0.2)),
      ),
      child: Row(
        children: [
          Icon(Icons.insert_drive_file, color: context.colorScheme.primary),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(fileName, style: const TextStyle(fontWeight: FontWeight.bold), maxLines: 1, overflow: TextOverflow.ellipsis),
                Text('${(fileSize / (1024 * 1024)).toStringAsFixed(2)} MB', style: context.textTheme.labelSmall),
              ],
            ),
          ),
          IconButton(
            icon: const Icon(Icons.close),
            onPressed: onRemove,
          ),
        ],
      ),
    );
  }
}
