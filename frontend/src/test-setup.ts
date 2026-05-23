import '@angular/compiler';

import { TestBed } from '@angular/core/testing';
import {
  Building2,
  CircleAlert,
  Copy,
  Eye,
  FileText,
  FolderOpen,
  History,
  Image,
  Lightbulb,
  LucideAngularModule,
  Pencil,
  Plus,
  RotateCcw,
  Save,
  Sparkles,
  Trash2,
  X,
} from 'lucide-angular';

const originalConfigureTestingModule = TestBed.configureTestingModule.bind(TestBed);

TestBed.configureTestingModule = (moduleDef) =>
  originalConfigureTestingModule({
    ...moduleDef,
    imports: [
      LucideAngularModule.pick({
        Building2,
        CircleAlert,
        Copy,
        Eye,
        FileText,
        FolderOpen,
        History,
        Image,
        Lightbulb,
        Pencil,
        Plus,
        RotateCcw,
        Save,
        Sparkles,
        Trash2,
        X,
      }),
      ...(moduleDef?.imports ?? []),
    ],
  });
