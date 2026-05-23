import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

import { CardComponent } from '@app/shared/components/card/card';
import { IconComponent } from '@app/shared/components/icon/icon';
import { CREATIVE_TYPE_OPTIONS } from '../../models/creative-generation.models';

@Component({
  selector: 'app-creative-type-selector',
  standalone: true,
  imports: [ReactiveFormsModule, CardComponent, IconComponent],
  templateUrl: './creative-type-selector.html',
  styleUrl: './creative-type-selector.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreativeTypeSelector {
  readonly form = input.required<FormGroup>();
  readonly disabled = input(false);

  protected readonly creativeTypeOptions = CREATIVE_TYPE_OPTIONS;
}
